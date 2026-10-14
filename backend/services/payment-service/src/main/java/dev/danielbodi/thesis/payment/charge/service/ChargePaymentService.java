package dev.danielbodi.thesis.payment.charge.service;

import dev.danielbodi.thesis.event.IntegrationEventPublisher;
import dev.danielbodi.thesis.fault.FaultInjector;
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
    private final IntegrationEventPublisher integrationEventPublisher;
    private final FaultInjector faultInjector;

    @Transactional
    public void charge(UUID reference, BigDecimal amount) {
        // a request already settled under another event id must not reach the provider again
        if (paymentRepository.existsByReference(reference)) {
            log.info("Payment for reference [{}] already recorded, charge skipped", reference);
            return;
        }

        final PspChargeResponse response = pspClient.charge(reference, amount);

        // S5: the provider has charged, nothing is committed yet
        faultInjector.reach("AFTER_PSP_CALL");

        final String providerReference = response.chargeId().toString();
        final Payment payment = paymentRepository.save(switch (response.status()) {
            case SUCCEEDED -> Payment.succeeded(reference, amount, providerReference);
            case FAILED -> Payment.failed(reference, amount, providerReference);
        });
        final PaymentStatus status = payment.getStatus();

        log.info("Payment [{}] recorded for reference: [{}] with status: [{}]",
                payment.getId(), reference, status);

        integrationEventPublisher.publish(switch (status) {
            case SUCCEEDED -> new PaymentSucceededEvent(reference.toString(), reference.toString());
            case FAILED -> new PaymentFailedEvent(reference.toString(), reference.toString());
        });
    }
}
