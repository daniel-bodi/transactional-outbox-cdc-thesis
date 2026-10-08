package dev.danielbodi.thesis.payment.common.psp;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record PspChargeRequest(UUID reference, BigDecimal amount) {
}
