package dev.danielbodi.thesis.measurement.scenario;

import lombok.Getter;

import java.util.Map;

/**
 * @author danielbodi
 */
@Getter
public enum Scenario {

    // S0-V0: naive dual write happy path (without designed faults)
    S0_V0(Variant.V0, Map.of(
            "DUAL_WRITE_ENABLED", "true",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "",
            "FAULT_TARGET_REACH_COUNT", "1",
            "PSP_IDEMPOTENCYKEY_ENABLED", "true")),

    // S0-V1: transactional outbox + cdc happy path (without designed faults)
    S0_V1(Variant.V1, Map.of(
            "DUAL_WRITE_ENABLED", "false",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "",
            "FAULT_TARGET_REACH_COUNT", "1",
            "PSP_IDEMPOTENCYKEY_ENABLED", "true"));


    private final Variant variant;
    private final Map<String, String> environment;

    Scenario(Variant variant, Map<String, String> environment) {
        this.variant = variant;
        this.environment = environment;
    }
}
