package dev.danielbodi.thesis.measurement.scenario;

import java.time.Duration;

/**
 * @author danielbodi
 */
public record PlannedOutage(String service, int atLoadRequest, Duration downFor) {
}
