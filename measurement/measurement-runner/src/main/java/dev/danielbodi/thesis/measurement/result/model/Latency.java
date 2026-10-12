package dev.danielbodi.thesis.measurement.result.model;

/**
 * @author danielbodi
 */
public record Latency(int count, Long p50, Long p95, Long max) {
}
