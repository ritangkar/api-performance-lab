package com.ritangkar.apiperf.repository;

import com.ritangkar.apiperf.entity.OrderUnindexed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderUnindexedRepository extends JpaRepository<OrderUnindexed, Long> {

    List<OrderUnindexed> findByCustomerIdAndStatus(Long customerId, String status);
}
