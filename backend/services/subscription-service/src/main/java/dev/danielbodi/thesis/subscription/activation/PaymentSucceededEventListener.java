package dev.danielbodi.thesis.subscription.activation;

import dev.danielbodi.thesis.fault.FaultInjector;
import dev.danielbodi.thesis.idempotency.annotation.Idempotent;
import dev.danielbodi.thesis.subscription.activation.service.ActivateSubscriptionService;
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

    private final ActivateSubscriptionService activateSubscriptionService;
    private final FaultInjector faultInjector;

    @Idempotent
    @KafkaListener(topics = "outbox.event.payment_succeeded")
    public void on(PaymentSucceededEvent event) {
        log.info("Received payment success: reference=[{}]", event.reference());

        activateSubscriptionService.activate(event.reference());

        // S5d: the outcome and its processed_events row are committed, the Kafka offset is not
        faultInjector.reachAfterCommit("AFTER_OUTCOME_COMMIT");
    }
}
