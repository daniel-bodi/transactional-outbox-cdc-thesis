package dev.danielbodi.thesis.measurement.evaluation;

import dev.danielbodi.thesis.measurement.database.PaymentDao;
import dev.danielbodi.thesis.measurement.database.PspDao;
import dev.danielbodi.thesis.measurement.database.StatusCounts;
import dev.danielbodi.thesis.measurement.database.SubscriptionDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

    private final SubscriptionDao subscriptionDao;
    private final PaymentDao paymentDao;
    private final PspDao pspDao;

    public Evaluation evaluate() {
        // the queries run one after the other, while a run that did not converge may still be moving:
        // each set is read before the one it is compared with that can only grow, so a change in between
        // cannot fake a violation (e.g. a payment completed after its subscription was read as PENDING)
        final Set<UUID> requested = subscriptionDao.paymentRequestReferences();
        final Set<UUID> paid = paymentDao.completedPaymentReferences();
        final Set<UUID> pending = subscriptionDao.pendingPaymentRequestReferences();
        final Map<UUID, Long> chargedCountsByReference = pspDao.chargeCountsByReference();
        final Set<UUID> charged = chargedCountsByReference.keySet();

        final Map<Invariant, InvariantResult> invariantResults = new EnumMap<>(Invariant.class);
        invariantResults.put(Invariant.NO_LOST_REQUEST, InvariantResult.of(difference(requested, charged)));
        invariantResults.put(Invariant.NO_LOST_OUTCOME, InvariantResult.of(intersection(paid, pending)));
        invariantResults.put(Invariant.NO_DUPLICATE_CHARGE, InvariantResult.of(chargedMoreThanOnce(chargedCountsByReference)));
        invariantResults.put(Invariant.NO_PHANTOM_CHARGE, InvariantResult.of(difference(charged, requested)));

        final StatusCounts statusCounts = subscriptionDao.statusCounts();
        final long totalChargeCount = chargedCountsByReference.values().stream().mapToLong(Long::longValue).sum();
        final Map<String, Long> duplicateDeliveries = new LinkedHashMap<>();
        duplicateDeliveries.put("subscription-service", subscriptionDao.duplicateDeliveries());
        duplicateDeliveries.put("payment-service", paymentDao.duplicateDeliveries());
        final long duplicateRequestCount = pspDao.duplicateRequests();

        return new Evaluation(
                invariantResults,
                statusCounts,
                paid.size(),
                totalChargeCount,
                duplicateDeliveries,
                duplicateRequestCount);
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
}
