package dev.danielbodi.thesis.measurement.evaluation.model;

/**
 * @author danielbodi
 */
public record Latency(int count, Long p50, Long p95, Long max) {
}
