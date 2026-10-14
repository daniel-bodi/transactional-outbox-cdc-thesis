package dev.danielbodi.thesis.measurement.evaluation.model;

import java.time.Instant;

/**
 * @author danielbodi
 */
public record Outage(
        String container,
        Integer exitCode,
        Instant diedAt,
        Instant healthyAt,
        Long unavailableMillis) {
}
