package dev.danielbodi.thesis.payment.charge;

import dev.danielbodi.thesis.idempotency.annotation.Idempotent;
import dev.danielbodi.thesis.payment.charge.service.ChargePaymentService;
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
public class PaymentRequestedEventListener {

    private final ChargePaymentService chargePaymentService;

    @Idempotent
    @KafkaListener(topics = "outbox.event.payment_requested")
    public void on(PaymentRequestedEvent request) {
        log.info("Received payment request: reference=[{}] amount=[{}]",
                request.reference(), request.amount());

        chargePaymentService.charge(request.reference(), request.amount());
    }
}
