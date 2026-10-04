# Portfolio Demo

**Demo level:** 3 — architecture walkthrough of real, measured numbers (not a live backend).

Backend performance work doesn't port to a client-side JavaScript demo
without misrepresenting what's being shown — there's no database, cache or
JVM to compare in a browser. Instead, `lab/demos/api-performance-lab.js`
presents the four real, measured comparisons from `docs/design-decisions.md`
(indexing, caching, pagination, concurrency) as an interactive before/after
panel — each backed by the exact numbers this project's own benchmark
scripts produced, clearly labeled as measured in a specific environment
rather than claimed as universal figures.

Running `mvn test` locally reproduces the equivalent comparisons against
this project's actual Spring Boot + H2 + Caffeine stack on your own
hardware — see the four `*BenchmarkTest` classes in `src/test`.
