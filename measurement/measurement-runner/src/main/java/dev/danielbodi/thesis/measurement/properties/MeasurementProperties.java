package dev.danielbodi.thesis.measurement.properties;

import dev.danielbodi.thesis.measurement.scenario.Scenario;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * @author danielbodi
 */
@Validated
@ConfigurationProperties("measurement")
public record MeasurementProperties(
        @NotNull @DefaultValue List<Scenario> scenarios,
        @Positive @DefaultValue("1") int repetitions,
        @NotNull Path composeDirectory,
        @NotNull Path resultsDirectory,
        @NotNull String kafkaConnectUrl,
        @NotNull String subscriptionServiceUrl,
        @Positive int loadCount,
        @Positive int loadRatePerSecond,
        @NotNull Duration warmUpTimeout,
        @NotNull Duration convergenceTimeout) {
}
