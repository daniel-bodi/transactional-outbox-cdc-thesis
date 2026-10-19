package dev.danielbodi.thesis.measurement.evaluation;

import dev.danielbodi.thesis.measurement.database.PaymentDao;
import dev.danielbodi.thesis.measurement.database.PspDao;
import dev.danielbodi.thesis.measurement.database.StatusCounts;
import dev.danielbodi.thesis.measurement.database.SubscriptionDao;
import dev.danielbodi.thesis.measurement.database.SubscriptionRow;
import dev.danielbodi.thesis.measurement.docker.ContainerEvent;
import dev.danielbodi.thesis.measurement.docker.ContainerEventRecorder;
import dev.danielbodi.thesis.measurement.evaluation.model.Evaluation;
import dev.danielbodi.thesis.measurement.evaluation.model.Invariant;
import dev.danielbodi.thesis.measurement.evaluation.model.InvariantResult;
import dev.danielbodi.thesis.measurement.evaluation.model.Latency;
import dev.danielbodi.thesis.measurement.evaluation.model.Outage;
import dev.danielbodi.thesis.measurement.evaluation.model.Timeline;
import dev.danielbodi.thesis.measurement.evaluation.model.Verdict;
import dev.danielbodi.thesis.measurement.load.RequestOutcome;
import dev.danielbodi.thesis.measurement.load.RequestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author danielbodi
 */
@Component
@RequiredArgsConstructor
public class Evaluator {

    private static final double PERCENTILE_50 = 0.5;
    private static final double PERCENTILE_95 = 0.95;

    private final SubscriptionDao subscriptionDao;
    private final PaymentDao paymentDao;
    private final PspDao pspDao;
    private final ConvergenceService convergenceService;
    private final ContainerEventRecorder containerEventRecorder;

    public Evaluation evaluate(List<RequestResult> requests, Duration convergenceTimeout) {
        // the run is evaluated once every subscription left PENDING, or the timeout is over
        final boolean converged = convergenceService.awaitNoPending(convergenceTimeout);

        // the queries run one after the other, while a run that did not converge may still be moving:
        // each set is read before the one it is compared with that can only grow, so a change in between
        // cannot fake a violation (e.g. a payment completed after its subscription was read as PENDING)
        final Set<UUID> requested = subscriptionDao.paymentRequestReferences();
        final Set<UUID> paid = paymentDao.completedPaymentReferences();
        final Set<UUID> pending = subscriptionDao.pendingPaymentRequestReferences();
        final Map<UUID, Long> chargedCountsByReference = pspDao.chargeCountsByReference();
        final Set<UUID> charged = chargedCountsByReference.keySet();

        final Map<Invariant, InvariantResult> invariantResults = new EnumMap<>(Invariant.class);
        invariantResults.put(Invariant.NO_LOST_REQUEST, invariantResult(difference(requested, charged)));
        invariantResults.put(Invariant.NO_LOST_OUTCOME, invariantResult(intersection(paid, pending)));
        invariantResults.put(Invariant.NO_DUPLICATE_CHARGE, invariantResult(chargedMoreThanOnce(chargedCountsByReference)));

        final long totalChargeCount = chargedCountsByReference.values().stream().mapToLong(Long::longValue).sum();
        final long duplicateRequestCount = pspDao.duplicateRequests();

        final List<SubscriptionRow> subscriptions = subscriptionDao.subscriptions();
        final StatusCounts statusCounts = statusCounts(subscriptions);

        final Map<RequestOutcome, Long> requestOutcomes = requestOutcomes(requests);
        final Latency latency = latency(requests, subscriptions);
        final Timeline timeline = timeline(containerEventRecorder.events(), requests, subscriptions);

        final Map<String, Long> duplicateDeliveries = new LinkedHashMap<>();
        duplicateDeliveries.put("subscription-service", subscriptionDao.duplicateDeliveries());
        duplicateDeliveries.put("payment-service", paymentDao.duplicateDeliveries());

        return new Evaluation(
                converged,
                invariantResults,
                statusCounts,
                paid.size(),
                totalChargeCount,
                duplicateDeliveries,
                duplicateRequestCount,
                requestOutcomes,
                latency,
                timeline,
                subscriptions);
    }

    private static InvariantResult invariantResult(Collection<UUID> violations) {
        return new InvariantResult(violations.isEmpty() ? Verdict.PASS : Verdict.FAIL, List.copyOf(violations));
    }

    private static Set<UUID> difference(Set<UUID> left, Set<UUID> right) {
        final Set<UUID> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    private static Set<UUID> intersection(Set<UUID> left, Set<UUID> right) {
        final Set<UUID> result = new HashSet<>(left);
        result.retainAll(right);
        return result;
    }

    private static Set<UUID> chargedMoreThanOnce(Map<UUID, Long> chargedCountsByReference) {
        return chargedCountsByReference.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static StatusCounts statusCounts(List<SubscriptionRow> subscriptions) {
        final Map<String, Long> counts = subscriptions.stream()
                .collect(Collectors.groupingBy(SubscriptionRow::status, Collectors.counting()));

        return new StatusCounts(
                counts.getOrDefault("PENDING", 0L),
                counts.getOrDefault("ACTIVE", 0L),
                counts.getOrDefault("FAILED", 0L));
    }

    private static Map<RequestOutcome, Long> requestOutcomes(List<RequestResult> requests) {
        final Map<RequestOutcome, Long> outcomes = new EnumMap<>(RequestOutcome.class);
        // every outcome is present, even with 0, so the result of every run has the same keys
        for (RequestOutcome outcome : RequestOutcome.values()) {
            outcomes.put(outcome, 0L);
        }
        for (RequestResult request : requests) {
            outcomes.merge(request.outcome(), 1L, Long::sum);
        }
        return outcomes;
    }

    private static Latency latency(List<RequestResult> requests, List<SubscriptionRow> subscriptions) {
        // the latency of the load subscriptions only: the warm-up ones were created on a cold stack
        final Set<UUID> loadCustomers = requests.stream()
                .map(RequestResult::customerId)
                .collect(Collectors.toSet());

        final List<Long> latencies = new ArrayList<>();
        for (SubscriptionRow subscriptionRow : subscriptions) {
            if (loadCustomers.contains(subscriptionRow.customerId()) && subscriptionRow.completedAt() != null) {
                final Duration elapsed = Duration.between(subscriptionRow.createdAt(), subscriptionRow.completedAt());
                latencies.add(elapsed.toMillis());
            }
        }

        if (latencies.isEmpty()) {
            return new Latency(0, null, null, null);
        }

        Collections.sort(latencies);
        return new Latency(
                latencies.size(),
                percentile(latencies, PERCENTILE_50),
                percentile(latencies, PERCENTILE_95),
                latencies.getLast());
    }

    private static long percentile(List<Long> sorted, double percentile) {
        // nearest-rank: e.g. p95 of 100 values is the 95th smallest
        final int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        return sorted.get(index);
    }

    private static Timeline timeline(List<ContainerEvent> events, List<RequestResult> requests,
                                     List<SubscriptionRow> subscriptions) {
        final List<Outage> outages = outages(events);
        final List<RequestResult> requestsDuringOutages = requestsDuringOutages(requests, outages);
        final Long recoveryMillis = recoveryMillis(outages, subscriptions);
        final Map<RequestOutcome, Long> requestOutcomes = requestOutcomes(requestsDuringOutages);
        final long committedWithoutCreatedResponse = committedWithoutCreatedResponse(requests, subscriptions);

        return new Timeline(
                outages,
                recoveryMillis,
                requestOutcomes,
                committedWithoutCreatedResponse,
                events);
    }

    private static List<Outage> outages(List<ContainerEvent> events) {
        final List<Outage> outages = new ArrayList<>();
        for (ContainerEvent containerEvent : events) {
            if (containerEvent.action().equals("die")) {
                // null if the container did not become healthy again until the end of the run
                final Instant healthyAt = events.stream()
                        .filter(event -> event.container().equals(containerEvent.container())
                                && event.action().equals("health_status: healthy")
                                && event.at().isAfter(containerEvent.at()))
                        .map(ContainerEvent::at)
                        .findFirst()
                        .orElse(null);

                final Long unavailableMillis = healthyAt != null
                        ? Duration.between(containerEvent.at(), healthyAt).toMillis()
                        : null;

                outages.add(new Outage(
                        containerEvent.container(),
                        containerEvent.exitCode(),
                        containerEvent.at(),
                        healthyAt,
                        unavailableMillis));
            }
        }
        return outages;
    }

    private static List<RequestResult> requestsDuringOutages(List<RequestResult> requests, List<Outage> outages) {
        final List<RequestResult> requestsDuringOutages = new ArrayList<>();
        for (RequestResult request : requests) {
            final Instant sentAt = request.sentAt();
            final boolean isRequestDuringOutage = outages.stream()
                    .anyMatch(outage -> sentAt.isAfter(outage.diedAt())
                            && (outage.healthyAt() == null || sentAt.isBefore(outage.healthyAt())));

            if (isRequestDuringOutage) {
                requestsDuringOutages.add(request);
            }
        }
        return requestsDuringOutages;
    }

    private static Long recoveryMillis(List<Outage> outages, List<SubscriptionRow> subscriptions) {
        // no outage, or a container never became healthy again: there is no recovery to measure
        if (outages.isEmpty() || outages.stream().anyMatch(outage -> outage.healthyAt() == null)) {
            return null;
        }

        final Instant firstDiedAt = outages.stream().map(Outage::diedAt).min(Comparator.naturalOrder()).orElseThrow();
        final Instant lastHealthyAt = outages.stream().map(Outage::healthyAt).max(Comparator.naturalOrder()).orElseThrow();

        // the search starts at the timestamp of the crash, so the subscriptions completed
        // before it (like the warm-up) do not affect the value
        Instant recoveredAt = firstDiedAt;
        for (SubscriptionRow subscription : subscriptions) {
            // the subscriptions created after every container was healthy again are normal traffic,
            // counting them would stretch T until the end of the load
            // the ones created before the crash stay, the one in flight at the crash is among them
            if (subscription.createdAt().isAfter(lastHealthyAt)) {
                continue;
            }

            // an affected subscription never completed means the system did not recover
            if (subscription.completedAt() == null) {
                return null;
            }

            // find the latest completion of the affected subscriptions
            if (subscription.completedAt().isAfter(recoveredAt)) {
                recoveredAt = subscription.completedAt();
            }
        }
        return Duration.between(firstDiedAt, recoveredAt).toMillis();
    }

    private static long committedWithoutCreatedResponse(List<RequestResult> requests, List<SubscriptionRow> subscriptions) {
        final Set<UUID> customerIds = subscriptions.stream()
                .map(SubscriptionRow::customerId)
                .collect(Collectors.toSet());

        long count = 0;
        for (RequestResult request : requests) {
            // matched by the customer id, every request uses a new one
            if (request.outcome() != RequestOutcome.CREATED && customerIds.contains(request.customerId())) {
                count++;
            }
        }
        return count;
    }
}
