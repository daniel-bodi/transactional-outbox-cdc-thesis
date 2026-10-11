package dev.danielbodi.thesis.measurement.configuration;

import dev.danielbodi.thesis.measurement.scenario.Scenario;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

/**
 * @author danielbodi
 */
@Validated
@ConfigurationProperties("measurement")
public record MeasurementProperties(
        @NotNull Scenario scenario,
        @NotNull Path composeDirectory,
        @NotNull String kafkaConnectUrl) {
}
