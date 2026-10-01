package dev.danielbodi.thesis.pspmock.charge.service;

import dev.danielbodi.thesis.pspmock.common.persistence.Charge;
import dev.danielbodi.thesis.pspmock.common.persistence.ChargeRepository;
import dev.danielbodi.thesis.pspmock.common.persistence.ChargeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author danielbodi
 */
@Service
@RequiredArgsConstructor
public class ChargeService {

    private final ChargeRepository chargeRepository;

    @Transactional
    public Charge charge(UUID reference, BigDecimal amount) {
        return chargeRepository.save(new Charge(reference, amount, ChargeStatus.SUCCEEDED));
    }
}
