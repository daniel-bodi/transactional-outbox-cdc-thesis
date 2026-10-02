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
public class PaymentSucceededEventListener {

    private final SubscriptionActivationService subscriptionActivationService;

    @KafkaListener(topics = "outbox.event.payment_succeeded")
    @Idempotent
    public void on(PaymentSucceededEvent event) {
        log.info("Received payment success: reference=[{}]", event.reference());

        subscriptionActivationService.activate(event.reference());
    }
}
