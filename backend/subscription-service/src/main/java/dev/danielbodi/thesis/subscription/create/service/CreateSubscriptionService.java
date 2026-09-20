package dev.danielbodi.thesis.subscription.create.service;

import dev.danielbodi.thesis.outbox.event.OutboxEventPublisher;
import dev.danielbodi.thesis.subscription.common.persistence.Plan;
import dev.danielbodi.thesis.subscription.common.persistence.PlanRepository;
import dev.danielbodi.thesis.subscription.common.persistence.Subscription;
import dev.danielbodi.thesis.subscription.common.persistence.SubscriptionRepository;
import dev.danielbodi.thesis.subscription.common.event.PaymentRequestedEvent;
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
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public UUID createSubscription(UUID customerId, UUID planId) {
        final Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("No such plan: " + planId));

        final Subscription subscription = new Subscription(customerId, plan);
        subscriptionRepository.save(subscription);

        outboxEventPublisher.publish(new PaymentRequestedEvent(
                subscription.getId().toString(),
                subscription.getId().toString(),
                plan.getAmount()));

        return subscription.getId();
    }
}
