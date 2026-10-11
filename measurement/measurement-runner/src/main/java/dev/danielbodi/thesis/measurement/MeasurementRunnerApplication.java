package dev.danielbodi.thesis.measurement;

import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import dev.danielbodi.thesis.measurement.run.MeasurementRun;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * @author danielbodi
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class MeasurementRunnerApplication implements ApplicationRunner {

    private final MeasurementProperties properties;
    private final MeasurementRun measurementRun;

    static void main(String[] args) {
        SpringApplication.run(MeasurementRunnerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        // one run per start: --measurement.scenario=S0-V1
        measurementRun.execute(properties.scenario());
    }
}
