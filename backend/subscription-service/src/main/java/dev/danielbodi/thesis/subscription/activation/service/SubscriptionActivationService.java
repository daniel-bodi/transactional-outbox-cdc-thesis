package dev.danielbodi.thesis.subscription.activation.service;

import dev.danielbodi.thesis.subscription.common.persistence.Subscription;
import dev.danielbodi.thesis.subscription.common.persistence.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionActivationService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void activate(UUID subscriptionId) {
        final Subscription subscription = find(subscriptionId);
        subscription.activate();

        log.info("Subscription [{}] activated", subscriptionId);
    }

    @Transactional
    public void fail(UUID subscriptionId) {
        final Subscription subscription = find(subscriptionId);
        subscription.fail();

        log.info("Subscription [{}] failed due to declined payment", subscriptionId);
    }

    private Subscription find(UUID subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("No such subscription: " + subscriptionId));
    }
}
