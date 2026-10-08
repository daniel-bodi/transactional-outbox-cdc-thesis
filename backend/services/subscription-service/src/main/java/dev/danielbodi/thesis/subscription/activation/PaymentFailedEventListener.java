package dev.danielbodi.thesis.subscription.activation;

import dev.danielbodi.thesis.fault.FaultInjector;
import dev.danielbodi.thesis.idempotency.annotation.Idempotent;
import dev.danielbodi.thesis.subscription.activation.service.FailSubscriptionService;
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

    private final FailSubscriptionService failSubscriptionService;
    private final FaultInjector faultInjector;

    @Idempotent
    @KafkaListener(topics = "outbox.event.payment_failed")
    public void on(PaymentFailedEvent event) {
        log.info("Received payment failure: reference=[{}]", event.reference());

        failSubscriptionService.fail(event.reference());

        // S5d: the outcome and its processed_events row are committed, the Kafka offset is not
        faultInjector.reachAfterCommit("AFTER_OUTCOME_COMMIT");
    }
}
