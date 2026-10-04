package com.ritangkar.apiperf.service;

import com.ritangkar.apiperf.entity.OrderIndexed;
import com.ritangkar.apiperf.entity.OrderUnindexed;
import com.ritangkar.apiperf.repository.OrderIndexedRepository;
import com.ritangkar.apiperf.repository.OrderUnindexedRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndexingLookupService {

    private final OrderIndexedRepository indexedRepository;
    private final OrderUnindexedRepository unindexedRepository;

    public IndexingLookupService(OrderIndexedRepository indexedRepository, OrderUnindexedRepository unindexedRepository) {
        this.indexedRepository = indexedRepository;
        this.unindexedRepository = unindexedRepository;
    }

    public List<OrderIndexed> findIndexed(Long customerId, String status) {
        return indexedRepository.findByCustomerIdAndStatus(customerId, status);
    }

    public List<OrderUnindexed> findUnindexed(Long customerId, String status) {
        return unindexedRepository.findByCustomerIdAndStatus(customerId, status);
    }

    public long totalOrderCount() {
        return indexedRepository.count();
    }
}
