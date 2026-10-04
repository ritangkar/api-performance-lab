package com.ritangkar.apiperf.entity;

import jakarta.persistence.*;

/**
 * Identical shape and data to {@link OrderUnindexed} — the only difference
 * is the composite index below. Comparing queries against these two tables
 * is how {@code IndexingBenchmarkTest} measures indexing's real effect
 * without needing two separate databases.
 */
@Entity
@Table(name = "orders_indexed", indexes = {
        @Index(name = "idx_customer_status", columnList = "customerId,status")
})
public class OrderIndexed {

    @Id
    private Long id;

    private Long customerId;
    private String status;
    private Double totalAmount;
    private String createdAt;

    protected OrderIndexed() {
    }

    public OrderIndexed(Long id, Long customerId, String status, Double totalAmount, String createdAt) {
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
