# API Performance Lab

A commerce-style Spring Boot backend built specifically to demonstrate —
and let you reproduce — four measurable backend performance techniques:
**indexing, caching, pagination and concurrent processing.** Every number
in this README was actually measured, with the script that measured it
committed alongside it.

Part of the [Commerce Engineering Lab](https://ritangkar.github.io/#commerce-lab) —
17 small, production-minded engineering capabilities by Ritangkar Dey.

---

## 1. Project Overview

Four independent before/after comparisons, each with real code implementing
both sides and a real, runnable test that reproduces the numbers:

1. Indexed vs. unindexed order lookup
2. Cached vs. uncached product detail lookup
3. Paginated vs. "return everything" product listing
4. Parallel vs. sequential bulk order recalculation

## 2. Problem

"We added an index / a cache / pagination" is a common line in a resume or
a portfolio, almost never backed by a number anyone actually measured. That
makes it hard to tell real backend engineering experience from a checklist.

## 3. Solution

Ship both sides of each comparison as real, running code against the same
data, plus a benchmark that measures — not asserts — the difference, so
the numbers are yours to reproduce, not mine to claim.

## 4. Key Features

- Two identical order tables (indexed / unindexed) with fixed-seed
  synthetic data, so the indexing comparison is apples-to-apples
- `@Cacheable` (Caffeine) product lookup with a genuinely expensive,
  CPU-bound computation behind it — caching it has a real effect to measure
- A naive "return everything" endpoint kept deliberately alongside the
  recommended paginated one, for direct comparison
- Sequential vs. parallel-stream bulk recalculation over an
  embarrassingly-parallel synthetic workload
- Four `*BenchmarkTest` JUnit classes that print real p50/p95/throughput
  numbers when you run them
- Standalone, dependency-free scripts in `/benchmarks` that produced every
  number quoted in `docs/design-decisions.md`, runnable on their own

## 5. Architecture

```
DataInitializer (fixed seed) → orders_indexed, orders_unindexed, products
        |                              |                    |
IndexingLookupService          ProductCatalogService   ProductRepository
        |                       (@Cacheable)                |
OrderLookupController                  |            ProductController
                                        └──────► (naive + paginated)

BulkOrderProcessingService (sequential + parallel) → BulkProcessingController
```

See [`docs/architecture.md`](docs/architecture.md) for the full diagram.

## 6. Technical Approach

- H2 in file mode (no external database process required) with Spring
  Data JPA
- Caffeine cache behind Spring's standard `@Cacheable` abstraction — no
  custom caching code
- Every comparison is measured with `System.nanoTime()`, not estimated
- See [`docs/design-decisions.md`](docs/design-decisions.md) for exactly
  how each number was produced, including where a number **wasn't**
  produced (connection pooling) and why

## 7. Design Decisions

The full, honest write-up — including the concurrency benchmark's honest
1.29x-on-one-core result, not a flattering fabricated one — is in
[`docs/design-decisions.md`](docs/design-decisions.md).

## 8. Sample Input

```
GET /api/orders/indexed?customerId=4821&status=SHIPPED
GET /api/orders/unindexed?customerId=4821&status=SHIPPED
GET /api/products/all
GET /api/products?page=0&size=20
GET /api/products/1042
POST /api/bulk/recalculate?mode=parallel&count=40000
```

## 9. Sample Output

```
$ mvn test -Dtest=IndexingBenchmarkTest
...
WITHOUT index: p50=X.XXX ms, p95=X.XXX ms, mean=X.XXX ms
WITH index:    p50=0.0XX ms, p95=0.0XX ms, mean=0.0XX ms
Speedup (mean): XXX.Xx
```

Exact figures depend on your machine — see Section 14.

## 10. How to Run

```bash
git clone <your-repo-url>
cd api-performance-lab
mvn spring-boot:run
```

First startup generates 50,000 rows in each order table plus 50,000
products (a few seconds); subsequent restarts detect existing data and
skip regeneration. API available at `http://localhost:8085`.

## 11. How to Test

```bash
mvn test
```

Includes ordinary unit tests (`BulkOrderProcessingServiceTest`) and the
four benchmark tests, which print their measured numbers to the console —
look for the `INFO` log lines from each `*BenchmarkTest` class.

To reproduce the standalone proof-of-concept numbers without Maven at all:

```bash
cd benchmarks
python3 sqlite_indexing_proof.py
java CacheBench.java
java ParallelBench.java
java PaginationBench.java
```

## 12. API Documentation

| Method | Path | Description |
|---|---|---|
| GET | `/api/orders/indexed` | `customerId`, `status` — queries the indexed table |
| GET | `/api/orders/unindexed` | Same query, unindexed table |
| GET | `/api/orders/count` | Row count per order table |
| GET | `/api/products/all` | Naive: entire catalog, no pagination |
| GET | `/api/products` | `page`, `size` (default 20, max 100) |
| GET | `/api/products/{id}` | Cached detail lookup |
| POST | `/api/bulk/recalculate` | `mode=sequential\|parallel`, `count` |

## 13. Portfolio Demo

**Demo level:** 3 — architecture walkthrough of the real measured numbers
above, not a live backend. See [`docs/demo.md`](docs/demo.md).

## 14. Limitations

- All numbers in this README and `docs/design-decisions.md` were measured
  in one specific development environment (notably: **1 available
  processor**, which materially limits the concurrency comparison) — your
  numbers will differ, which is exactly why every benchmark is reproducible
  rather than hardcoded as a universal claim
- The indexing proof-of-concept uses SQLite, not this project's actual H2
  stack, because this development environment has no network access to
  fetch Maven/H2 dependencies — `IndexingBenchmarkTest` is what reproduces
  the comparison against the real stack, on your machine
- Connection-pool sizing is a documented configuration decision, not a
  benchmarked one — see Section 7 for why

## 15. Future Enhancements

- A `/actuator/metrics`-backed dashboard for the cache hit rate over time
- A load-testing profile (e.g. Gatling) for the connection-pool question
  this project currently only documents analytically
- A slow-query log comparison alongside the timing-based benchmark

## 16. License

MIT — see [`LICENSE`](LICENSE).
