package dev.danielbodi.thesis.payment.charge;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record PaymentRequestedEvent(UUID reference, BigDecimal amount) {
}
