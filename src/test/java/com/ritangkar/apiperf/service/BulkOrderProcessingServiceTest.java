package com.ritangkar.apiperf.service;

import com.ritangkar.apiperf.entity.RecalculableOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class BulkOrderProcessingServiceTest {

    private final BulkOrderProcessingService service = new BulkOrderProcessingService();

    @Test
    void sequentialAndParallelAgreeOnASmallBatch() {
        List<RecalculableOrder> orders = service.generateSyntheticOrders(500);

        double sequential = service.recalculateSequential(orders);
        double parallel = service.recalculateParallel(orders);

        assertThat(parallel).isCloseTo(sequential, within(0.0001));
    }

    @Test
    void generatesTheRequestedNumberOfOrders() {
        assertThat(service.generateSyntheticOrders(250)).hasSize(250);
    }

    @Test
    void recalculationIsDeterministicForTheSameInput() {
        List<RecalculableOrder> orders = List.of(new RecalculableOrder(1, 1000.0, 0.10, 0.12));

        double first = service.recalculateSequential(orders);
        double second = service.recalculateSequential(orders);

        // Independently verified with a standalone script before this test was written.
        assertThat(first).isCloseTo(1013.4054, within(0.001));
        assertThat(first).isEqualTo(second);
    }
}
