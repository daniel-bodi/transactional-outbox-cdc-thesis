package dev.danielbodi.thesis.measurement.run;

import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import dev.danielbodi.thesis.measurement.result.ResultService;
import dev.danielbodi.thesis.measurement.scenario.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MeasurementCampaign {

    private static final DateTimeFormatter CAMPAIGN_ID_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final MeasurementProperties properties;
    private final MeasurementRun measurementRun;
    private final ResultService resultService;

    public void execute() {
        final List<Scenario> scenarios = properties.scenarios().isEmpty()
                ? List.of(Scenario.values())
                : properties.scenarios();
        final int runCount = scenarios.size() * properties.repetitions();

        final Instant startedAt = Instant.now();
        final String campaignId = CAMPAIGN_ID_FORMAT.format(startedAt);

        log.info("Campaign [{}] started with scenarios {} and [{}] repetitions, total [{}] runs",
                campaignId, scenarios, properties.repetitions(), runCount);

        // in rounds instead of the repetitions of a scenario one after the other,
        // so a drift of the machine during the campaign is spread over every scenario
        final List<FailedRun> failedRuns = new ArrayList<>();
        int runNumber = 0;
        for (int repetition = 1; repetition <= properties.repetitions(); repetition++) {
            for (Scenario scenario : scenarios) {
                runNumber++;

                log.info("Campaign [{}] run [{}/{}]: scenario [{}], repetition [{}/{}]",
                        campaignId, runNumber, runCount, scenario, repetition, properties.repetitions());
                try {
                    measurementRun.execute(new RunContext(campaignId, scenario, repetition));
                } catch (RuntimeException e) {
                    // a failed run does not stop the campaign, the stack is torn down by the run itself
                    log.error("Run of scenario [{}], repetition [{}] failed", scenario, repetition, e);
                    failedRuns.add(new FailedRun(scenario, repetition, e.toString()));
                }
            }
        }

        resultService.saveCampaign(campaignId, scenarios, startedAt, runCount, failedRuns);
    }
}
