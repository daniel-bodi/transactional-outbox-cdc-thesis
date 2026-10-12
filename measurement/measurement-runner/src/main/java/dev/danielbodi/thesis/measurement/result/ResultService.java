package dev.danielbodi.thesis.measurement.result;

import dev.danielbodi.thesis.measurement.database.SubscriptionRow;
import dev.danielbodi.thesis.measurement.evaluation.model.Evaluation;
import dev.danielbodi.thesis.measurement.evaluation.model.Invariant;
import dev.danielbodi.thesis.measurement.evaluation.model.InvariantResult;
import dev.danielbodi.thesis.measurement.load.RequestResult;
import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import dev.danielbodi.thesis.measurement.result.model.EntityCounts;
import dev.danielbodi.thesis.measurement.result.model.InvariantOutcome;
import dev.danielbodi.thesis.measurement.result.model.Latency;
import dev.danielbodi.thesis.measurement.result.model.Metrics;
import dev.danielbodi.thesis.measurement.result.model.RawData;
import dev.danielbodi.thesis.measurement.result.model.RequestEntry;
import dev.danielbodi.thesis.measurement.result.model.RunMetadata;
import dev.danielbodi.thesis.measurement.result.model.RunResult;
import dev.danielbodi.thesis.measurement.result.model.SubscriptionEntry;
import dev.danielbodi.thesis.measurement.result.model.SubscriptionStatusCounts;
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
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

    private static final DateTimeFormatter RUN_ID_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final MeasurementProperties properties;
    private final JsonMapper jsonMapper;

    public void save(Scenario scenario, Instant startedAt, List<RequestResult> requests, Evaluation evaluation) {
        final RunResult result = new RunResult(
                metadata(scenario, startedAt),
                evaluation.converged(),
                invariants(evaluation),
                counts(evaluation),
                metrics(evaluation),
                rawData(requests, evaluation));

        logSummary(result);
        writeResultFile(result);
    }

    private RunMetadata metadata(Scenario scenario, Instant startedAt) {
        return new RunMetadata(
                "%s-%s".formatted(scenario, RUN_ID_TIME_FORMAT.format(startedAt)),
                scenario,
                scenario.getVariant(),
                scenario.getEnvironment(),
                properties.warmUpCount(),
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

    private static RawData rawData(List<RequestResult> requests, Evaluation evaluation) {
        return new RawData(
                requests.stream().map(ResultService::requestEntry).toList(),
                evaluation.subscriptions().stream().map(ResultService::subscriptionEntry).toList());
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

    private static void logSummary(RunResult result) {
        log.info("Run [{}] finished, converged: {}", result.metadata().id(), result.converged());
        result.invariants().forEach((invariant, outcome) -> log.info("  {} {} {} ({} violations)",
                outcome.thesisId(), invariant, outcome.verdict(), outcome.violationCount()));
        log.info("  subscriptions {}, payments {}, charges {}",
                result.counts().subscriptions(), result.counts().payments(), result.counts().charges());
        log.info("  duplicate deliveries {}, duplicate PSP requests {}",
                result.metrics().duplicateDeliveries(), result.metrics().duplicateRequests());
        log.info("  requests {}, latency {}", result.metrics().requestOutcomes(), result.metrics().latencyMillis());
    }

    private void writeResultFile(RunResult result) {
        try {
            final Path directory = properties.resultsDirectory().toAbsolutePath().normalize();
            Files.createDirectories(directory);
            final Path file = directory.resolve(result.metadata().id() + ".json");
            jsonMapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), result);
            log.info("Result written to {}", file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
