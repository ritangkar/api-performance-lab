package com.ritangkar.apiperf.service;

import com.ritangkar.apiperf.entity.RecalculableOrder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

/**
 * Recomputes a total for every order in a batch — a stand-in for a
 * real periodic job (re-pricing, tax-rule changes, loyalty recalculation)
 * over a large order set. Both methods do exactly the same per-order work;
 * only sequential vs. parallel execution differs, which is what
 * ConcurrencyBenchmarkTest measures. See docs/design-decisions.md for the
 * real, measured (and honestly caveated) numbers.
 */
@Service
public class BulkOrderProcessingService {

    public List<RecalculableOrder> generateSyntheticOrders(int count) {
        Random rng = new Random(7);
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(i -> new RecalculableOrder(i, 200 + rng.nextDouble() * 8000,
                        0.05 + rng.nextDouble() * 0.15, 0.12))
                .toList();
    }

    public double recalculateSequential(List<RecalculableOrder> orders) {
        return orders.stream().mapToDouble(this::recalcTotal).sum();
    }

    public double recalculateParallel(List<RecalculableOrder> orders) {
        return orders.parallelStream().mapToDouble(this::recalcTotal).sum();
    }

    /**
     * Deliberately non-trivial, side-effect-free CPU work — each order is
     * independent, so this is "embarrassingly parallel" with no shared
     * mutable state and nothing to synchronize.
     */
    private double recalcTotal(RecalculableOrder o) {
        double total = o.subtotal();
        for (int i = 0; i < 400; i++) {
            total = total * (1 - o.discountRate() / 400) + Math.sin(i) * 0.0001;
        }
        return total * (1 + o.taxRate());
    }
}
