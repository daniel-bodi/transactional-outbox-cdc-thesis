package dev.danielbodi.thesis.measurement.run;

import dev.danielbodi.thesis.measurement.docker.DockerCompose;
import dev.danielbodi.thesis.measurement.scenario.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeasurementRun {

    private final DockerCompose dockerCompose;

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
            log.info("Stack is up");

        } finally {
            dockerCompose.down();
        }
    }
}
