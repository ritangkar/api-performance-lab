package com.ritangkar.apiperf.benchmark;

import com.ritangkar.apiperf.entity.RecalculableOrder;
import com.ritangkar.apiperf.service.BulkOrderProcessingService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Compares sequential vs. parallel-stream bulk recalculation over the same
 * synthetic order batch. This test deliberately does NOT assert that
 * parallel is faster — on a single-core machine it usually isn't, and
 * asserting a hardware-dependent outcome would make the test flaky. It
 * asserts only that both paths compute the identical result (correctness),
 * and logs the timing for you to read.
 *
 * In this development environment (1 available processor, confirmed by
 * Runtime.getRuntime().availableProcessors()), the equivalent standalone
 * benchmark measured only a 1.29x speedup — expected on a single core.
 * On real multi-core hardware this same, embarrassingly-parallel workload
 * should scale much closer to the core count. Run this test on your own
 * machine to see your own number — see docs/design-decisions.md.
 */
@SpringBootTest
class ConcurrencyBenchmarkTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyBenchmarkTest.class);
    private static final int ORDER_COUNT = 40_000;

    @Autowired
    private BulkOrderProcessingService bulkOrderProcessingService;

    @Test
    void sequentialAndParallelProduceTheSameResult() {
        List<RecalculableOrder> orders = bulkOrderProcessingService.generateSyntheticOrders(ORDER_COUNT);

        // warm up JIT — not measured
        bulkOrderProcessingService.recalculateSequential(orders.subList(0, 2000));
        bulkOrderProcessingService.recalculateParallel(orders.subList(0, 2000));

        long t0 = System.nanoTime();
        double sequentialSum = bulkOrderProcessingService.recalculateSequential(orders);
        long sequentialMs = (System.nanoTime() - t0) / 1_000_000;

        long t1 = System.nanoTime();
        double parallelSum = bulkOrderProcessingService.recalculateParallel(orders);
        long parallelMs = (System.nanoTime() - t1) / 1_000_000;

        log.info("Available processors: {}", Runtime.getRuntime().availableProcessors());
        log.info("Orders: {}", ORDER_COUNT);
        log.info("Sequential: {} ms", sequentialMs);
        log.info("Parallel:   {} ms", parallelMs);
        log.info("Speedup: {}x (hardware-dependent — see class javadoc)",
                String.format("%.2f", sequentialMs / (double) Math.max(1, parallelMs)));

        assertThat(parallelSum).isCloseTo(sequentialSum, within(0.001));
    }
}
