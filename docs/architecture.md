# Architecture

## Components

| Component | Responsibility |
|---|---|
| `DataInitializer` | Generates fixed-seed synthetic data into both order tables and the product table at startup |
| `OrderIndexed` / `OrderUnindexed` | Identical schema and data; only the composite index differs |
| `IndexingLookupService` | Wraps both order repositories behind one comparison-friendly interface |
| `ProductCatalogService` | `@Cacheable` hot-path lookup with a genuinely expensive (CPU-bound) computation |
| `BulkOrderProcessingService` | Sequential vs. parallel-stream bulk recalculation over synthetic orders |
| `ProductController` | Naive "return everything" endpoint alongside the recommended paginated one |
| `OrderLookupController`, `BulkProcessingController` | Expose the indexing and concurrency comparisons over HTTP |
| `benchmark/*BenchmarkTest` | Real, runnable JUnit tests that reproduce every number in docs/design-decisions.md |

## Data Flow

```mermaid
flowchart TD
    Start[App startup] --> Init[DataInitializer<br/>fixed seed 20260918]
    Init --> OI[(orders_indexed<br/>+ composite index)]
    Init --> OU[(orders_unindexed<br/>no extra index)]
    Init --> P[(products)]

    OI --> ILS[IndexingLookupService]
    OU --> ILS
    ILS --> OLC[OrderLookupController]

    P --> PR[ProductRepository]
    PR --> PC[ProductController /all, paginated]
    P --> PCS[ProductCatalogService<br/>@Cacheable]
    PCS --> PC

    BOP[BulkOrderProcessingService<br/>sequential + parallel] --> BPC[BulkProcessingController]
```

### ASCII fallback

```
DataInitializer (fixed seed) --> orders_indexed, orders_unindexed, products
                                       |                |            |
                              IndexingLookupService  (used by both)  ProductRepository / ProductCatalogService
                                       |                                        |
                              OrderLookupController                   ProductController (naive + paginated)

BulkOrderProcessingService (sequential + parallel) --> BulkProcessingController
```

## Important Interfaces

- `OrderIndexed` and `OrderUnindexed` are deliberately two separate entities
  mapped to two separate tables with identical generated data — this is
  what makes the indexing comparison fair and reproducible without needing
  two databases or a way to "turn off" an index at query time.
- `BulkOrderProcessingService`'s per-order computation (`recalcTotal`) has
  no shared mutable state between orders, which is what makes the parallel
  version both correct and genuinely scalable with core count — not just
  parallel-looking.

## External Dependencies

- Spring Web, Spring Data JPA, Spring Cache (runtime)
- H2 (embedded, file-mode) — no external database server required
- Caffeine — the cache implementation behind Spring's `@Cacheable`
- Spring Boot Test (JUnit 5, Mockito, AssertJ — test scope only)

## Decision Points

- **Why two tables instead of one table queried two ways**: JPA doesn't
  offer a clean way to "ignore" an index for one query but not another on
  the same table, and forcing that at the SQL level would make the code
  harder to read than just maintaining two clearly-labeled tables.
- **Why the concurrency benchmark asserts correctness, not speed**: see
  `ConcurrencyBenchmarkTest`'s javadoc — asserting a hardware-dependent
  speedup would make the test flaky on constrained CI runners. Reporting
  the number (rather than gating on it) is the honest choice.
- **Why HikariCP's pool is small by default**: documented directly in
  `application.yml`, next to the setting itself, so the rationale travels
  with the config rather than living only in a separate doc.
