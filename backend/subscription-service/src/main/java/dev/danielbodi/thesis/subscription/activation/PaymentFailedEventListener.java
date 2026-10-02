package dev.danielbodi.thesis.subscription.activation;

import dev.danielbodi.thesis.idempotency.annotation.Idempotent;
import dev.danielbodi.thesis.subscription.activation.service.SubscriptionActivationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFailedEventListener {

    private final SubscriptionActivationService subscriptionActivationService;

    @KafkaListener(topics = "outbox.event.payment_failed")
    @Idempotent
    public void on(PaymentFailedEvent event) {
        log.info("Received payment failure: reference=[{}]", event.reference());

        subscriptionActivationService.fail(event.reference());
    }
}
