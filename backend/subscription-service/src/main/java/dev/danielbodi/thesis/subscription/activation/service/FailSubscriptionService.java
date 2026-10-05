package dev.danielbodi.thesis.subscription.activation.service;

import dev.danielbodi.thesis.subscription.common.persistence.PaymentRequestRepository;
import dev.danielbodi.thesis.subscription.common.persistence.Subscription;
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
public class FailSubscriptionService {

    private final PaymentRequestRepository paymentRequestRepository;

    @Transactional
    public void fail(UUID reference) {
        final Subscription subscription = paymentRequestRepository.findById(reference)
                .orElseThrow(() -> new IllegalArgumentException("No such payment request: " + reference))
                .getSubscription();
        subscription.fail();

        log.info("Subscription [{}] failed due to declined payment request [{}]", subscription.getId(), reference);
    }
}
