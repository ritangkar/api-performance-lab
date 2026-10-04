package com.ritangkar.apiperf.web.dto;

public record RecalculationResult(String mode, int orderCount, long durationMs, double sum, int availableProcessors) {
}
