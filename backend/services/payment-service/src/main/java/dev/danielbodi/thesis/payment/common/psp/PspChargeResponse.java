package dev.danielbodi.thesis.payment.common.psp;

import java.util.UUID;

/**
 * @author danielbodi
 */
public record PspChargeResponse(UUID chargeId, PspChargeStatus status) {
}
