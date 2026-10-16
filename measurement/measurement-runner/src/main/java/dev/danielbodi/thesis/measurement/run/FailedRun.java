package dev.danielbodi.thesis.measurement.run;

import dev.danielbodi.thesis.measurement.scenario.Scenario;

/**
 * @author danielbodi
 */
public record FailedRun(Scenario scenario, int repetition, String error) {
}
