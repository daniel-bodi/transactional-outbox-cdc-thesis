package dev.danielbodi.thesis.measurement.run;

import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import dev.danielbodi.thesis.measurement.docker.DockerCompose;
import dev.danielbodi.thesis.measurement.load.LoadGenerator;
import dev.danielbodi.thesis.measurement.load.RequestOutcome;
import dev.danielbodi.thesis.measurement.load.RequestResult;
import dev.danielbodi.thesis.measurement.load.WarmUpService;
import dev.danielbodi.thesis.measurement.scenario.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeasurementRun {

    private final MeasurementProperties properties;
    private final DockerCompose dockerCompose;
    private final WarmUpService warmUpService;
    private final LoadGenerator loadGenerator;

    public void execute(Scenario scenario) {
        log.info("Run of scenario [{}] with environment [{}]", scenario, scenario.getEnvironment());

        try {
            // a run always starts from empty volumes
            // anything left of the stack (a dev stack, an aborted run) is removed
            dockerCompose.down();

            dockerCompose.up(scenario.getEnvironment());
            if (scenario.getVariant().usesConnectors()) {
                dockerCompose.registerConnectors();
            }

            log.info("The docker compose stack is up and running");

            warmUpService.warmUp(properties.warmupCount(), properties.loadRatePerSecond());

            final List<RequestResult> requests = loadGenerator.generate(properties.loadCount(), properties.loadRatePerSecond());
            log.info("Load completed: {}", outcomes(requests));

        } finally {
            dockerCompose.down();
        }
    }

    private static Map<RequestOutcome, Long> outcomes(List<RequestResult> requests) {
        return requests.stream().collect(Collectors.groupingBy(RequestResult::outcome, Collectors.counting()));
    }
}
