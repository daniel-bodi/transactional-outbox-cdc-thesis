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
public class ActivateSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void activate(UUID subscriptionId) {
        final Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("No such subscription: " + subscriptionId));
        subscription.activate();

        log.info("Subscription [{}] activated", subscriptionId);
    }
}
