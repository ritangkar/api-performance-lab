package com.ritangkar.apiperf.benchmark;

import com.ritangkar.apiperf.service.IndexingLookupService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the exact same (customerId, status) lookup against the indexed and
 * unindexed order tables (identical data, ~50,000 rows each by default) and
 * prints a p50/p95 report.
 *
 * A SQLite proof-of-concept of this same comparison, on a 100,000-row
 * table, measured a ~465x speedup (p50 2.96ms -> 0.006ms) — see
 * docs/design-decisions.md for that methodology and numbers. This test is
 * what reproduces the equivalent comparison for this project's own H2
 * schema on your machine; exact figures will differ from the SQLite
 * reference and from machine to machine.
 */
@SpringBootTest
class IndexingBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(IndexingBenchmarkTest.class);
    private static final int SAMPLE_SIZE = 200;
    private static final String[] STATUSES = {"PLACED", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED"};

    @Autowired
    private IndexingLookupService indexingLookupService;

    @Test
    void indexedLookupIsNotSlowerThanUnindexedOnTheSameData() {
        Random rng = new Random(99);
        List<Long> customerIds = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            customerIds.add((long) (1 + rng.nextInt(10_000)));
            statuses.add(STATUSES[rng.nextInt(STATUSES.length)]);
        }

        // warm up (JIT + H2 page cache) — not measured
        for (int i = 0; i < 20; i++) {
            indexingLookupService.findIndexed(customerIds.get(i), statuses.get(i));
            indexingLookupService.findUnindexed(customerIds.get(i), statuses.get(i));
        }

        long[] unindexedTimes = new long[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            long t0 = System.nanoTime();
            indexingLookupService.findUnindexed(customerIds.get(i), statuses.get(i));
            unindexedTimes[i] = System.nanoTime() - t0;
        }

        long[] indexedTimes = new long[SAMPLE_SIZE];
        for (int i = 0; i < SAMPLE_SIZE; i++) {
            long t0 = System.nanoTime();
            indexingLookupService.findIndexed(customerIds.get(i), statuses.get(i));
            indexedTimes[i] = System.nanoTime() - t0;
        }

        Report unindexed = Report.of(unindexedTimes);
        Report indexed = Report.of(indexedTimes);

        log.info("Total orders per table: {}", indexingLookupService.totalOrderCount());
        log.info("WITHOUT index: p50={} ms, p95={} ms, mean={} ms", unindexed.p50Ms(), unindexed.p95Ms(), unindexed.meanMs());
        log.info("WITH index:    p50={} ms, p95={} ms, mean={} ms", indexed.p50Ms(), indexed.p95Ms(), indexed.meanMs());
        log.info("Speedup (mean): {}x", String.format("%.1f", unindexed.meanMs() / Math.max(0.0001, indexed.meanMs())));

        assertThat(indexed.meanMs()).isLessThanOrEqualTo(unindexed.meanMs());
    }

    private record Report(double p50Ms, double p95Ms, double meanMs) {
        static Report of(long[] nanos) {
            long[] sorted = nanos.clone();
            java.util.Arrays.sort(sorted);
            double p50 = sorted[sorted.length / 2] / 1_000_000.0;
            double p95 = sorted[(int) (sorted.length * 0.95)] / 1_000_000.0;
            double mean = java.util.Arrays.stream(sorted).average().orElse(0) / 1_000_000.0;
            return new Report(round(p50), round(p95), round(mean));
        }

        static double round(double v) {
            return Math.round(v * 1000.0) / 1000.0;
        }
    }
}
