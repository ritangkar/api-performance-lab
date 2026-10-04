package com.ritangkar.apiperf.repository;

import com.ritangkar.apiperf.entity.OrderIndexed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderIndexedRepository extends JpaRepository<OrderIndexed, Long> {

    List<OrderIndexed> findByCustomerIdAndStatus(Long customerId, String status);
}
