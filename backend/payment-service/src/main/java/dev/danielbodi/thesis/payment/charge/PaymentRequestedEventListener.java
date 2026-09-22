package dev.danielbodi.thesis.payment.charge;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author danielbodi
 */
@Slf4j
@Component
public class PaymentRequestedEventListener {

    @KafkaListener(topics = "outbox.event.payment_requested")
    public void on(PaymentRequestedEvent request) {
        log.info("Received payment request: reference=[{}] amount=[{}]",
                request.reference(), request.amount());
    }
}
