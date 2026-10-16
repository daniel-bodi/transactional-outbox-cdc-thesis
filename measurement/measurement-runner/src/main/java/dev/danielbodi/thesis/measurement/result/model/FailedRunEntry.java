package dev.danielbodi.thesis.measurement.result.model;

import dev.danielbodi.thesis.measurement.scenario.Scenario;

/**
 * @author danielbodi
 */
public record FailedRunEntry(Scenario scenario, int repetition, String error) {
}
