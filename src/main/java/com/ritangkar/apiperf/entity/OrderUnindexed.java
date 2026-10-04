package com.ritangkar.apiperf.entity;

import jakarta.persistence.*;

/**
 * Identical shape and data to {@link OrderIndexed}, deliberately left
 * without a composite index on (customerId, status) so
 * {@code IndexingBenchmarkTest} has a genuine "before" to compare against.
 * This is not a realistic production table — it exists purely as the
 * control side of the benchmark.
 */
@Entity
@Table(name = "orders_unindexed")
public class OrderUnindexed {

    @Id
    private Long id;

    private Long customerId;
    private String status;
    private Double totalAmount;
    private String createdAt;

    protected OrderUnindexed() {
    }

    public OrderUnindexed(Long id, Long customerId, String status, Double totalAmount, String createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public String getStatus() { return status; }
    public Double getTotalAmount() { return totalAmount; }
    public String getCreatedAt() { return createdAt; }
}
