package dev.danielbodi.thesis.subscription.create.service;

import dev.danielbodi.thesis.event.IntegrationEventPublisher;
import dev.danielbodi.thesis.fault.FaultInjector;
import dev.danielbodi.thesis.subscription.common.event.PaymentRequestedEvent;
import dev.danielbodi.thesis.subscription.common.persistence.PaymentRequest;
import dev.danielbodi.thesis.subscription.common.persistence.PaymentRequestRepository;
import dev.danielbodi.thesis.subscription.common.persistence.Plan;
import dev.danielbodi.thesis.subscription.common.persistence.PlanRepository;
import dev.danielbodi.thesis.subscription.common.persistence.Subscription;
import dev.danielbodi.thesis.subscription.common.persistence.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @author danielbodi
 */
@Service
@RequiredArgsConstructor
public class CreateSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final IntegrationEventPublisher integrationEventPublisher;
    private final FaultInjector faultInjector;

    @Transactional
    public UUID createSubscription(UUID customerId, UUID planId) {
        final Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("No such plan: " + planId));

        final Subscription subscription = subscriptionRepository.save(Subscription.pending(customerId, plan));
        final PaymentRequest paymentRequest = paymentRequestRepository.save(PaymentRequest.forSubscription(subscription));

        integrationEventPublisher.publish(new PaymentRequestedEvent(
                subscription.getId().toString(),
                paymentRequest.getId().toString(),
                paymentRequest.getAmount()));

        // S1: the subscription is committed, the HTTP response has not been sent yet
        faultInjector.reachAfterCommit("AFTER_SUBSCRIPTION_COMMIT");

        return subscription.getId();
    }
}
