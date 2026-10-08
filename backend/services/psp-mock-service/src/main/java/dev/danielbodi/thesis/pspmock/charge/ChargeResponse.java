package dev.danielbodi.thesis.pspmock.charge;

import dev.danielbodi.thesis.pspmock.common.persistence.ChargeStatus;

import java.util.UUID;

/**
 * @author danielbodi
 */
public record ChargeResponse(UUID chargeId, ChargeStatus status) {
}
