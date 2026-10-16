package dev.danielbodi.thesis.measurement.result;

import dev.danielbodi.thesis.measurement.database.SubscriptionRow;
import dev.danielbodi.thesis.measurement.docker.ContainerEvent;
import dev.danielbodi.thesis.measurement.evaluation.model.Evaluation;
import dev.danielbodi.thesis.measurement.evaluation.model.Invariant;
import dev.danielbodi.thesis.measurement.evaluation.model.InvariantResult;
import dev.danielbodi.thesis.measurement.load.RequestResult;
import dev.danielbodi.thesis.measurement.load.WarmUpService;
import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import dev.danielbodi.thesis.measurement.run.FailedRun;
import dev.danielbodi.thesis.measurement.run.RunContext;
import dev.danielbodi.thesis.measurement.result.model.CampaignResult;
import dev.danielbodi.thesis.measurement.result.model.ContainerEventEntry;
import dev.danielbodi.thesis.measurement.result.model.EntityCounts;
import dev.danielbodi.thesis.measurement.result.model.FailedRunEntry;
import dev.danielbodi.thesis.measurement.result.model.InvariantOutcome;
import dev.danielbodi.thesis.measurement.result.model.Latency;
import dev.danielbodi.thesis.measurement.result.model.Metrics;
import dev.danielbodi.thesis.measurement.result.model.Outage;
import dev.danielbodi.thesis.measurement.result.model.RawData;
import dev.danielbodi.thesis.measurement.result.model.RequestEntry;
import dev.danielbodi.thesis.measurement.result.model.RunMetadata;
import dev.danielbodi.thesis.measurement.result.model.RunResult;
import dev.danielbodi.thesis.measurement.result.model.SubscriptionEntry;
import dev.danielbodi.thesis.measurement.result.model.SubscriptionStatusCounts;
import dev.danielbodi.thesis.measurement.result.model.Timeline;
import dev.danielbodi.thesis.measurement.scenario.Scenario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResultService {

    private final MeasurementProperties properties;
    private final JsonMapper jsonMapper;

    public void saveRun(RunContext context, Instant startedAt, List<RequestResult> requests, Evaluation evaluation) {
        final RunResult result = new RunResult(
                metadata(context, startedAt),
                evaluation.converged(),
                invariants(evaluation),
                counts(evaluation),
                metrics(evaluation),
                timeline(evaluation),
                rawData(requests, evaluation));

        logRunSummary(result);
        writeFile(context.campaignId(), result.metadata().id() + ".json", result);
    }

    public void saveCampaign(String campaignId, List<Scenario> scenarios, Instant startedAt, int runCount,
                             List<FailedRun> failedRuns) {
        final CampaignResult result = new CampaignResult(
                campaignId,
                scenarios,
                properties.repetitions(),
                WarmUpService.WARM_UP_COUNT,
                properties.loadCount(),
                properties.loadRatePerSecond(),
                startedAt,
                Instant.now(),
                runCount,
                failedRuns.stream().map(ResultService::failedRunEntry).toList());

        logCampaignSummary(result);
        writeFile(campaignId, "campaign.json", result);
    }

    private RunMetadata metadata(RunContext context, Instant startedAt) {
        final Scenario scenario = context.scenario();
        return new RunMetadata(
                "%s-%d".formatted(scenario, context.repetition()),
                context.campaignId(),
                context.repetition(),
                scenario,
                scenario.getVariant(),
                scenario.getEnvironment(),
                WarmUpService.WARM_UP_COUNT,
                properties.loadCount(),
                properties.loadRatePerSecond(),
                startedAt,
                Instant.now());
    }

    private static Map<Invariant, InvariantOutcome> invariants(Evaluation evaluation) {
        final Map<Invariant, InvariantOutcome> invariants = new EnumMap<>(Invariant.class);
        evaluation.invariantResults().forEach((invariant, result) -> invariants.put(invariant, invariantOutcome(invariant, result)));
        return invariants;
    }

    private static InvariantOutcome invariantOutcome(Invariant invariant, InvariantResult result) {
        return new InvariantOutcome(invariant.getThesisId(), result.verdict(), result.violations().size(), result.violations());
    }

    private static EntityCounts counts(Evaluation evaluation) {
        return new EntityCounts(
                new SubscriptionStatusCounts(
                        evaluation.statusCounts().pending(),
                        evaluation.statusCounts().active(),
                        evaluation.statusCounts().failed()),
                evaluation.payments(),
                evaluation.charges());
    }

    private static Metrics metrics(Evaluation evaluation) {
        return new Metrics(
                new Latency(
                        evaluation.latency().count(),
                        evaluation.latency().p50(),
                        evaluation.latency().p95(),
                        evaluation.latency().max()),
                evaluation.duplicateDeliveries(),
                evaluation.duplicateRequests(),
                evaluation.requestOutcomes());
    }

    private static Timeline timeline(Evaluation evaluation) {
        return new Timeline(
                // a lambda instead of a method reference: the evaluation Outage has the same name
                evaluation.timeline().outages().stream().map(outage -> new Outage(
                        outage.container(),
                        outage.exitCode(),
                        outage.diedAt(),
                        outage.healthyAt(),
                        outage.unavailableMillis())).toList(),
                evaluation.timeline().recoveryMillis(),
                evaluation.timeline().requestOutcomesDuringOutages(),
                evaluation.timeline().committedWithoutCreatedResponse());
    }

    private static RawData rawData(List<RequestResult> requests, Evaluation evaluation) {
        return new RawData(
                requests.stream().map(ResultService::requestEntry).toList(),
                evaluation.subscriptions().stream().map(ResultService::subscriptionEntry).toList(),
                evaluation.timeline().containerEvents().stream().map(ResultService::containerEventEntry).toList());
    }

    private static RequestEntry requestEntry(RequestResult request) {
        return new RequestEntry(
                request.index(),
                request.customerId(),
                request.sentAt(),
                request.finishedAt(),
                request.outcome(),
                request.httpStatus(),
                request.error());
    }

    private static SubscriptionEntry subscriptionEntry(SubscriptionRow subscription) {
        return new SubscriptionEntry(
                subscription.subscriptionId(),
                subscription.customerId(),
                subscription.paymentReference(),
                subscription.status(),
                subscription.createdAt(),
                subscription.completedAt());
    }

    private static FailedRunEntry failedRunEntry(FailedRun failedRun) {
        return new FailedRunEntry(failedRun.scenario(), failedRun.repetition(), failedRun.error());
    }

    private static ContainerEventEntry containerEventEntry(ContainerEvent event) {
        return new ContainerEventEntry(event.at(), event.container(), event.action(), event.exitCode());
    }

    private static void logRunSummary(RunResult result) {
        log.info("Run [{}] finished, converged: {}", result.metadata().id(), result.converged());
        result.invariants().forEach((invariant, outcome) -> log.info("  {} {} {} ({} violations)",
                outcome.thesisId(), invariant, outcome.verdict(), outcome.violationCount()));
        log.info("  subscriptions {}, payments {}, charges {}",
                result.counts().subscriptions(), result.counts().payments(), result.counts().charges());
        log.info("  duplicate deliveries {}, duplicate PSP requests {}",
                result.metrics().duplicateDeliveries(), result.metrics().duplicateRequests());
        log.info("  requests {}, latency {}", result.metrics().requestOutcomes(), result.metrics().latencyMillis());
        result.timeline().outages().forEach(outage -> log.info("  outage of {}: unavailable for {} ms",
                outage.container(), outage.unavailableMillis()));
        log.info("  recovery {} ms, requests during outages {}, committed without 201: {}",
                result.timeline().recoveryMillis(), result.timeline().requestOutcomesDuringOutages(),
                result.timeline().committedWithoutCreatedResponse());
    }

    private static void logCampaignSummary(CampaignResult result) {
        log.info("Campaign [{}] finished, [{}] of [{}] runs succeeded",
                result.id(), result.runCount() - result.failedRuns().size(), result.runCount());
        result.failedRuns().forEach(failedRun -> log.info("  failed: scenario [{}], repetition [{}]: {}",
                failedRun.scenario(), failedRun.repetition(), failedRun.error()));
    }

    private void writeFile(String campaignId, String fileName, Object content) {
        try {
            final Path directory = properties.resultsDirectory().resolve(campaignId).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            final Path file = directory.resolve(fileName);
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), content);
            log.info("Result written to {}", file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
