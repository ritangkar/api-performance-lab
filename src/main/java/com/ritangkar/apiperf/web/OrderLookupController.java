package com.ritangkar.apiperf.web;

import com.ritangkar.apiperf.entity.OrderIndexed;
import com.ritangkar.apiperf.entity.OrderUnindexed;
import com.ritangkar.apiperf.service.IndexingLookupService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderLookupController {

    private final IndexingLookupService indexingLookupService;

    public OrderLookupController(IndexingLookupService indexingLookupService) {
        this.indexingLookupService = indexingLookupService;
    }

    /** Query path hitting the composite-indexed table — see IndexingBenchmarkTest. */
    @GetMapping("/indexed")
    public List<OrderIndexed> findIndexed(@RequestParam Long customerId, @RequestParam String status) {
        return indexingLookupService.findIndexed(customerId, status);
    }

    /** Identical query, identical data, against the table with no composite index. */
    @GetMapping("/unindexed")
    public List<OrderUnindexed> findUnindexed(@RequestParam Long customerId, @RequestParam String status) {
        return indexingLookupService.findUnindexed(customerId, status);
    }

    @GetMapping("/count")
    public long count() {
        return indexingLookupService.totalOrderCount();
    }
}
