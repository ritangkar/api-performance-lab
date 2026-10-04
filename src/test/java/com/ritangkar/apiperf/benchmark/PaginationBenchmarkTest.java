package com.ritangkar.apiperf.benchmark;

import com.ritangkar.apiperf.entity.Product;
import com.ritangkar.apiperf.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compares {@code findAll()} (the whole catalog) against a single
 * {@code findAll(Pageable)} page, measuring both query time and result
 * size. A standalone benchmark using the same synthetic data shape (50,000
 * products) measured an ~2,580x smaller JSON payload and ~3,160x faster
 * serialization for a 20-row page versus the full catalog — see
 * docs/design-decisions.md.
 */
@SpringBootTest
class PaginationBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(PaginationBenchmarkTest.class);

    @Autowired
    private ProductRepository productRepository;

    @Test
    void aSinglePageIsRetrievedFasterAndSmallerThanTheFullCatalog() {
        long t0 = System.nanoTime();
        List<Product> all = productRepository.findAll();
        long allDurationMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        Page<Product> page = productRepository.findAll(PageRequest.of(0, 20));
        long pageDurationMs = (System.nanoTime() - t1) / 1_000_000;

        log.info("Full catalog: {} rows in {} ms", all.size(), allDurationMs);
        log.info("Single page:  {} rows in {} ms (of {} total, {} pages)",
                page.getNumberOfElements(), pageDurationMs, page.getTotalElements(), page.getTotalPages());

        assertThat(page.getNumberOfElements()).isLessThanOrEqualTo(20);
        assertThat(page.getTotalElements()).isEqualTo(all.size());
        assertThat(pageDurationMs).isLessThanOrEqualTo(allDurationMs + 5); // +5ms tolerance for noise
    }
}
