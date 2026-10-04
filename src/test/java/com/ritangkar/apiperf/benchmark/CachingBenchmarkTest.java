package com.ritangkar.apiperf.benchmark;

import com.ritangkar.apiperf.service.ProductCatalogService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calls the same {@code @Cacheable} lookup for a small set of "hot"
 * product ids, first with a cold cache and then warm, and reports the
 * speedup. A standalone (non-Spring) version of this exact comparison,
 * with the same CPU-bound per-lookup cost, measured a 9.7x speedup over
 * 300 requests to 30 unique items in this development environment — see
 * docs/design-decisions.md.
 */
@SpringBootTest
class CachingBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(CachingBenchmarkTest.class);
    private static final int UNIQUE_PRODUCTS = 10;
    private static final int TOTAL_CALLS = 100;

    @Autowired
    private ProductCatalogService catalogService;

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cachedLookupsAreNotSlowerThanUncachedOnes() {
        var cache = cacheManager.getCache("productDetails");
        assertThat(cache).isNotNull();
        cache.clear();

        long[] ids = new long[TOTAL_CALLS];
        for (int i = 0; i < TOTAL_CALLS; i++) ids[i] = i % UNIQUE_PRODUCTS;

        long coldStart = System.nanoTime();
        for (long id : ids) catalogService.getProductDetails(id);
        long coldDurationMs = (System.nanoTime() - coldStart) / 1_000_000;

        // cache is now fully warm for these UNIQUE_PRODUCTS ids — repeat the same sequence
        long warmStart = System.nanoTime();
        for (long id : ids) catalogService.getProductDetails(id);
        long warmDurationMs = (System.nanoTime() - warmStart) / 1_000_000;

        log.info("Unique products: {}, total calls: {}", UNIQUE_PRODUCTS, TOTAL_CALLS);
        log.info("First pass (cold + populating cache): {} ms", coldDurationMs);
        log.info("Second pass (fully warm cache):        {} ms", warmDurationMs);
        log.info("Speedup: {}x", String.format("%.1f", coldDurationMs / (double) Math.max(1, warmDurationMs)));

        assertThat(warmDurationMs).isLessThanOrEqualTo(coldDurationMs);
    }
}
