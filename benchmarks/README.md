# Standalone benchmark scripts

These produced the specific numbers quoted in docs/design-decisions.md.
None of them are part of the Spring Boot application; they're kept here so
every number in the docs is independently reproducible without needing to
run the full app.

- `sqlite_indexing_proof.py` — indexing proof-of-concept (Python stdlib only, no deps)
  ```
  python3 sqlite_indexing_proof.py
  ```
- `CacheBench.java`, `ParallelBench.java`, `PaginationBench.java` — plain Java, no dependencies
  ```
  java CacheBench.java
  java ParallelBench.java
  java PaginationBench.java
  ```
  (Java 11+ supports running a single .java file directly, no compilation step needed.)

The application's own `src/test/java/.../benchmark/*BenchmarkTest.java`
classes reproduce the equivalent comparisons against this project's actual
Spring Boot + H2 + Caffeine stack — run them with `mvn test`.
