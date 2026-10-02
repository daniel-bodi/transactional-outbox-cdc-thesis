package dev.danielbodi.thesis.payment.charge.service;

import dev.danielbodi.thesis.outbox.event.OutboxEventPublisher;
import dev.danielbodi.thesis.payment.common.event.PaymentFailedEvent;
import dev.danielbodi.thesis.payment.common.event.PaymentSucceededEvent;
import dev.danielbodi.thesis.payment.common.persistence.Payment;
import dev.danielbodi.thesis.payment.common.persistence.PaymentRepository;
import dev.danielbodi.thesis.payment.common.persistence.PaymentStatus;
import dev.danielbodi.thesis.payment.common.psp.PspChargeResponse;
import dev.danielbodi.thesis.payment.common.psp.PspClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargePaymentService {

    private final PspClient pspClient;
    private final PaymentRepository paymentRepository;
    private final OutboxEventPublisher outboxEventPublisher;

    @Transactional
    public void charge(UUID reference, BigDecimal amount) {
        final PspChargeResponse response = pspClient.charge(reference, amount);

        final PaymentStatus status = switch (response.status()) {
            case SUCCEEDED -> PaymentStatus.SUCCEEDED;
            case FAILED -> PaymentStatus.FAILED;
        };
        final Payment payment = paymentRepository.save(
                new Payment(reference, amount, status, response.chargeId().toString()));

        log.info("Payment [{}] recorded for reference: [{}] with status: [{}]",
                payment.getId(), reference, status);

        outboxEventPublisher.publish(switch (status) {
            case SUCCEEDED -> new PaymentSucceededEvent(reference.toString(), reference.toString());
            case FAILED -> new PaymentFailedEvent(reference.toString(), reference.toString());
        });
    }
}
