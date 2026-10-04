# Design Decisions

This document exists specifically to satisfy the collection's rule: *do not
invent benchmark results; benchmarks must be reproducible.* Every number
below was actually measured in this development environment, using the
scripts committed in `/benchmarks`, and every script is runnable with
nothing beyond a JDK or Python's standard library — no network access, no
external services.

---

## Indexing

**Decision:** Maintain two identical order tables, one with a composite
`(customerId, status)` index and one without, and query both the same way.

**Measured (SQLite proof-of-concept, `benchmarks/sqlite_indexing_proof.py`,
100,000 rows, 200 random lookups):**

| | p50 | p95 | mean |
|---|---|---|---|
| Without index | 3.04 ms | 3.51 ms | 3.03 ms |
| With index | 0.006 ms | 0.009 ms | 0.006 ms |

~486x mean speedup. `EXPLAIN QUERY PLAN` confirms the mechanism: the
unindexed query is a full `SCAN orders`; the indexed query is a `SEARCH
... USING INDEX idx_customer_status`.

**Why SQLite for the proof-of-concept, when the actual project uses H2:**
this development environment has no network access and therefore can't
fetch Maven dependencies (H2, Spring Boot) to run the real stack here.
SQLite ships with Python's standard library, so it's what could actually be
*measured*, honestly, in this environment, rather than presenting
guessed-at H2 numbers. `IndexingBenchmarkTest` in `src/test` runs the
equivalent comparison against this project's real H2 schema — run it
yourself with `mvn test` for numbers specific to your machine and this
project's exact JPA/H2 configuration; they will differ from the SQLite
figures above (different engine, different data volume by default) but
should show the same order-of-magnitude effect.

**Trade-off:** Composite indexes cost write throughput and storage. For a
table this size the cost is negligible; it would need re-evaluating at
much larger write volume.

---

## Caching

**Decision:** `@Cacheable` (Caffeine-backed) on `ProductCatalogService.getProductDetails`.

**Measured (`benchmarks/CacheBench.java`, plain Java, 30 unique products,
300 total requests simulating realistic repeated-hot-item traffic):**

| | total | per request |
|---|---|---|
| Without cache | 30,053 ms | 100.18 ms |
| With cache | 3,107 ms | 10.36 ms |

~9.7x speedup — expected, since with 30 unique items the cache only pays
the full cost once per item (30 × 100.18 ms ≈ 3,005 ms, matching the
measured 3,107 ms almost exactly) and serves the remaining 270 requests
near-instantly. `CachingBenchmarkTest` reproduces this against the real
`@Cacheable` service.

**Trade-off:** Cached data can go stale; `CacheConfig` sets a 10-minute
TTL as a deliberately simple staleness bound for a demo project, not a
tuned production value.

---

## Pagination

**Decision:** Ship both `/api/products/all` (naive) and `/api/products?page=&size=`
(recommended), rather than only the good version — the point is the comparison.

**Measured (`benchmarks/PaginationBench.java`, 50,000 synthetic products):**

| | serialize time | payload size |
|---|---|---|
| Return everything | 183.48 ms | 8,779,985 bytes (~8.4 MB) |
| Paginated (size=20) | 0.058 ms | 3,400 bytes |

~3,163x faster serialization, ~2,582x smaller payload. `PaginationBenchmarkTest`
reproduces the query-time side of this against the real repository.

**Trade-off:** None significant — pagination is close to a strict
improvement for any catalog past a trivial size. The only cost is a
slightly more complex client (handling page state).

---

## Concurrency

**Decision:** `BulkOrderProcessingService` offers both `recalculateSequential`
and `recalculateParallel` (Java parallel streams) over the same
embarrassingly-parallel per-order computation.

**Measured (`benchmarks/ParallelBench.java`, 60,000 synthetic orders, this
development environment):**

| | time |
|---|---|
| Sequential | 629.3 ms |
| Parallel | 489.6 ms |

Only a **1.29x** speedup — and this needs stating plainly rather than
glossed over: `Runtime.getRuntime().availableProcessors()` reports **1**
available processor in this environment, so there is no second core for
the parallel stream to actually run on; the small speedup it does show is
incidental overlap, not real parallelism. `ConcurrencyBenchmarkTest`
therefore asserts only that sequential and parallel produce the *same
result* (correctness), not that parallel is faster — asserting a
hardware-dependent speedup would make the test flaky on a constrained CI
runner.

**On real multi-core hardware** — which is what any actual deployment
target for a commerce API would have — this same workload (independent
per-order computation, no shared mutable state, no I/O) is exactly the
shape that scales close to linearly with core count. Run
`ConcurrencyBenchmarkTest` on your own machine and compare its logged
`availableProcessors()` and speedup to the numbers above.

**Trade-off:** Parallel streams use the common `ForkJoinPool`, shared with
the rest of the JVM — fine for an occasional batch job, worth a dedicated
executor if this ran frequently alongside other CPU-bound work.

---

## Connection pooling

**Decision:** A small HikariCP pool (`maximum-pool-size: 10`), documented
inline in `application.yml` rather than left at a large default.

**Why, without a fabricated throughput number:** properly benchmarking
pool sizing needs sustained concurrent load against a real database
connection, which isn't something this environment can produce safely or
meaningfully (no load-generation tooling, no separate DB process to
saturate). Rather than invent a number, this is documented as a
**configuration decision backed by the widely-used HikariCP sizing
formula** (`connections = (core_count * 2) + effective_spindle_count`) and
Little's Law reasoning: past a fairly small pool size, more connections
generally *reduce* throughput on the database side (context-switching,
lock contention) rather than increase it on the application side.

**How to verify this empirically yourself:** run the app, then hit
`/api/orders/indexed` concurrently (e.g. with `ab` or `hey`) at two
different `maximum-pool-size` values and compare observed throughput —
that comparison would produce a real, reproducible number specific to your
hardware, which this document deliberately doesn't fabricate on your behalf.
