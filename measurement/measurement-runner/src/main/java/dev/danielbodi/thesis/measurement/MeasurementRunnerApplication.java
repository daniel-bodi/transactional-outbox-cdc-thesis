package dev.danielbodi.thesis.measurement;

import dev.danielbodi.thesis.measurement.run.MeasurementCampaign;
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

    private final MeasurementCampaign measurementCampaign;

    static void main(String[] args) {
        SpringApplication.run(MeasurementRunnerApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        // one campaign per start, a single run is a campaign of one scenario: --measurement.scenarios=S0_V1
        // with the default repetition count of 1
        measurementCampaign.execute();
    }
}
