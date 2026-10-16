package dev.danielbodi.thesis.measurement.run;

import dev.danielbodi.thesis.measurement.scenario.Scenario;

/**
 * @author danielbodi
 */
public record RunContext(String campaignId, Scenario scenario, int repetition) {
}
