package dev.danielbodi.thesis.pspmock.charge.service;

import dev.danielbodi.thesis.pspmock.common.persistence.Charge;
import dev.danielbodi.thesis.pspmock.common.persistence.ChargeRepository;
import dev.danielbodi.thesis.pspmock.common.persistence.ChargeStatus;
import dev.danielbodi.thesis.pspmock.configuration.IdempotencyKeyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeService {

    private final ChargeRepository chargeRepository;
    private final IdempotencyKeyProperties idempotencyKeyProperties;

    @Transactional
    public Charge charge(UUID reference, BigDecimal amount, String idempotencyKey) {
        // with idempotency key handling disabled, every request is a new charge
        // e.g. an external service without idempotency support
        if (!idempotencyKeyProperties.enabled()) {
            return execute(Charge.withoutIdempotencyKey(reference, amount, ChargeStatus.SUCCEEDED));
        }

        final Optional<Charge> existing = chargeRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            // a duplicate request gets the original charge back instead of a new one
            final Charge charge = existing.get();
            charge.incrementDuplicateRequests();
            log.info("Idempotency key [{}] already used by charge [{}], duplicate request not charged",
                    idempotencyKey, charge.getId());

            return charge;
        }

        return execute(Charge.withIdempotencyKey(reference, amount, ChargeStatus.SUCCEEDED, idempotencyKey));
    }

    private Charge execute(Charge charge) {
        final Charge executed = chargeRepository.save(charge);
        log.info("Charge [{}] executed for reference: [{}] with status: [{}]",
                executed.getId(), executed.getReference(), executed.getStatus());

        return executed;
    }
}
