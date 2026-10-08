package dev.danielbodi.thesis.pspmock.charge;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record ChargeRequest(UUID reference, BigDecimal amount) {
}
