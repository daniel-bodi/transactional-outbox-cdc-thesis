package dev.danielbodi.thesis.measurement.docker;

import dev.danielbodi.thesis.measurement.scenario.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StackService {

    private final DockerCompose dockerCompose;

    public void setUp(Scenario scenario) {
        // a run always starts from empty volumes
        // anything left of the stack (a dev stack, an aborted run) is removed
        dockerCompose.down();

        dockerCompose.up(scenario.getEnvironment());
        if (scenario.getVariant().usesConnectors()) {
            dockerCompose.registerConnectors();
        }

        log.info("The docker compose stack is up and running");
    }

    public void tearDown() {
        dockerCompose.down();
    }
}
