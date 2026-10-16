package dev.danielbodi.thesis.measurement.run;

import dev.danielbodi.thesis.measurement.docker.ContainerKiller;
import dev.danielbodi.thesis.measurement.docker.StackService;
import dev.danielbodi.thesis.measurement.evaluation.Evaluator;
import dev.danielbodi.thesis.measurement.evaluation.model.Evaluation;
import dev.danielbodi.thesis.measurement.load.LoadGenerator;
import dev.danielbodi.thesis.measurement.load.RequestResult;
import dev.danielbodi.thesis.measurement.load.WarmUpService;
import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import dev.danielbodi.thesis.measurement.result.ResultService;
import dev.danielbodi.thesis.measurement.scenario.PlannedOutage;
import dev.danielbodi.thesis.measurement.scenario.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeasurementRun {

    private final MeasurementProperties properties;
    private final StackService stackService;
    private final ContainerKiller containerKiller;
    private final WarmUpService warmUpService;
    private final LoadGenerator loadGenerator;
    private final Evaluator evaluator;
    private final ResultService resultService;

    public void execute(RunContext context) {
        final Scenario scenario = context.scenario();
        log.info("Run of scenario [{}] started with environment [{}]", scenario, scenario.getEnvironment());
        final Instant startedAt = Instant.now();

        try {
            stackService.setUp(scenario);

            warmUpService.warmUp(properties.loadRatePerSecond(), properties.warmUpTimeout());

            final Optional<PlannedOutage> plannedOutage = scenario.getPlannedOutage();
            plannedOutage.ifPresent(outage -> containerKiller.start(outage, properties.loadRatePerSecond()));
            final List<RequestResult> requests = loadGenerator.generate(properties.loadCount(), properties.loadRatePerSecond());
            plannedOutage.ifPresent(_ -> containerKiller.await());

            final Evaluation evaluation = evaluator.evaluate(requests, properties.convergenceTimeout());
            resultService.saveRun(context, startedAt, requests, evaluation);
        } finally {
            stackService.tearDown();
        }

        log.info("Run of scenario [{}] finished", scenario);
    }
}
