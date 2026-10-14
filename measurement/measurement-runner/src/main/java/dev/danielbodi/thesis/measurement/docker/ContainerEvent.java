package dev.danielbodi.thesis.measurement.docker;

import java.time.Instant;

/**
 * @author danielbodi
 */
public record ContainerEvent(Instant at, String container, String action, Integer exitCode) {
}
