package com.ritangkar.apiperf.entity;

/**
 * A lightweight, non-persisted order line used only by
 * {@code BulkOrderProcessingService}'s sequential-vs-parallel benchmark.
 * It is intentionally not a JPA entity: the point of that benchmark is
 * CPU-bound recomputation, not database I/O, so persistence would only
 * dilute what's being measured.
 */
public record RecalculableOrder(long id, double subtotal, double discountRate, double taxRate) {
}
