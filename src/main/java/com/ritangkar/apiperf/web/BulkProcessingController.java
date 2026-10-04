package com.ritangkar.apiperf.web;

import com.ritangkar.apiperf.entity.RecalculableOrder;
import com.ritangkar.apiperf.service.BulkOrderProcessingService;
import com.ritangkar.apiperf.web.dto.RecalculationResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bulk")
public class BulkProcessingController {

    private final BulkOrderProcessingService bulkOrderProcessingService;

    public BulkProcessingController(BulkOrderProcessingService bulkOrderProcessingService) {
        this.bulkOrderProcessingService = bulkOrderProcessingService;
    }

    /**
     * Recalculates {@code count} synthetic orders using the requested mode
     * and reports wall-clock time. See ConcurrencyBenchmarkTest and
     * docs/design-decisions.md for the honestly-caveated measured numbers
     * (this environment's available core count materially affects the
     * result — the endpoint reports it for exactly that reason).
     */
    @PostMapping("/recalculate")
    public RecalculationResult recalculate(@RequestParam(defaultValue = "sequential") String mode,
                                            @RequestParam(defaultValue = "20000") int count) {
        int boundedCount = Math.max(100, Math.min(count, 200_000));
        List<RecalculableOrder> orders = bulkOrderProcessingService.generateSyntheticOrders(boundedCount);

        long start = System.nanoTime();
        double result = "parallel".equalsIgnoreCase(mode)
                ? bulkOrderProcessingService.recalculateParallel(orders)
                : bulkOrderProcessingService.recalculateSequential(orders);
        long durationMs = (System.nanoTime() - start) / 1_000_000;

        return new RecalculationResult(mode, boundedCount, durationMs, result,
                Runtime.getRuntime().availableProcessors());
    }
}
