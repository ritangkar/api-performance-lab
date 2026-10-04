This project generates its own synthetic data programmatically at startup
(see src/main/java/.../config/DataInitializer.java) rather than loading a
static file, so that the row count is configurable (apiperf.data.order-count
/ product-count in application.yml) while staying exactly reproducible —
the generator uses a fixed random seed (20260918L).

The standalone scripts in /benchmarks (not this folder) are what produced
the specific numbers quoted in docs/design-decisions.md, and are fully
reproducible on their own — see benchmarks/README.md.
