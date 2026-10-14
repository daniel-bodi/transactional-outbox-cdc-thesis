package dev.danielbodi.thesis.measurement.result.model;

import java.time.Instant;

/**
 * @author danielbodi
 */
public record ContainerEventEntry(Instant at, String container, String action, Integer exitCode) {
}
