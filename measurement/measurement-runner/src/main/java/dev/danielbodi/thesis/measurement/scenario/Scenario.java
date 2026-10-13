package dev.danielbodi.thesis.measurement.scenario;

import dev.danielbodi.thesis.measurement.load.WarmUpService;
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
            "PSP_IDEMPOTENCYKEY_ENABLED", "true")),

    // S1-V0: the subscription-service crashes right after a subscription commit, before the dual write send
    S1_V0(Variant.V0, Map.of(
            "DUAL_WRITE_ENABLED", "true",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "AFTER_SUBSCRIPTION_COMMIT",
            "FAULT_TARGET_REACH_COUNT", afterWarmUp(50),
            "PSP_IDEMPOTENCYKEY_ENABLED", "true")),

    // S1-V1: the subscription-service crashes right after a subscription commit, the outbox row is committed with it
    S1_V1(Variant.V1, Map.of(
            "DUAL_WRITE_ENABLED", "false",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "AFTER_SUBSCRIPTION_COMMIT",
            "FAULT_TARGET_REACH_COUNT", afterWarmUp(50),
            "PSP_IDEMPOTENCYKEY_ENABLED", "true"));


    private final Variant variant;
    private final Map<String, String> environment;

    Scenario(Variant variant, Map<String, String> environment) {
        this.variant = variant;
        this.environment = environment;
    }

    // the crash point counts the warm-up requests too, e.g. afterWarmUp(50) crashes at the 50th load request
    private static String afterWarmUp(int loadRequest) {
        return String.valueOf(WarmUpService.WARM_UP_COUNT + loadRequest);
    }
}
