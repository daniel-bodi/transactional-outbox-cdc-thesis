package dev.danielbodi.thesis.measurement.scenario;

import dev.danielbodi.thesis.measurement.load.WarmUpService;
import lombok.AccessLevel;
import lombok.Getter;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

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
            "PSP_IDEMPOTENCYKEY_ENABLED", "true")),

    // S5b-V1: the payment-service crashes after the provider call, the provider has no idempotency key support
    S5B_V1_KEY_OFF(Variant.V1, Map.of(
            "DUAL_WRITE_ENABLED", "false",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "AFTER_PSP_CALL",
            "FAULT_TARGET_REACH_COUNT", afterWarmUp(50),
            "PSP_IDEMPOTENCYKEY_ENABLED", "false")),

    // S5b-V1: the payment-service crashes after the provider call, the provider has idempotency key support
    S5B_V1_KEY_ON(Variant.V1, Map.of(
            "DUAL_WRITE_ENABLED", "false",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "AFTER_PSP_CALL",
            "FAULT_TARGET_REACH_COUNT", afterWarmUp(50),
            "PSP_IDEMPOTENCYKEY_ENABLED", "true")),

    // S5d-V1: the subscription-service crashes after the outcome commit, before the Kafka offset commit
    S5D_V1(Variant.V1, Map.of(
            "DUAL_WRITE_ENABLED", "false",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "AFTER_OUTCOME_COMMIT",
            "FAULT_TARGET_REACH_COUNT", afterWarmUp(50),
            "PSP_IDEMPOTENCYKEY_ENABLED", "true")),

    // S2-V1: the kafka-connect (debezium) is killed at the 50th load request and started again 10 s later
    S2_V1(Variant.V1, Map.of(
            "DUAL_WRITE_ENABLED", "false",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "",
            "FAULT_TARGET_REACH_COUNT", "1",
            "PSP_IDEMPOTENCYKEY_ENABLED", "true"),
            new PlannedOutage("kafka-connect", 50, Duration.ofSeconds(10))),

    // S3-V0: the kafka broker is killed at the 50th load request and started again 10 s later, the dual write send fails
    S3_V0(Variant.V0, Map.of(
            "DUAL_WRITE_ENABLED", "true",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "",
            "FAULT_TARGET_REACH_COUNT", "1",
            "PSP_IDEMPOTENCYKEY_ENABLED", "true"),
            new PlannedOutage("kafka", 50, Duration.ofSeconds(10))),

    // S3-V1: the kafka broker is killed at the 50th load request and started again 10 s later, the outbox rows wait in the WAL
    S3_V1(Variant.V1, Map.of(
            "DUAL_WRITE_ENABLED", "false",
            "FAULT_ACTIVE_CRASH_POINT_NAME", "",
            "FAULT_TARGET_REACH_COUNT", "1",
            "PSP_IDEMPOTENCYKEY_ENABLED", "true"),
            new PlannedOutage("kafka", 50, Duration.ofSeconds(10)));

    private final Variant variant;
    private final Map<String, String> environment;
    // null if the scenario has no planned outage
    @Getter(AccessLevel.NONE)
    private final PlannedOutage plannedOutage;

    Scenario(Variant variant, Map<String, String> environment) {
        this(variant, environment, null);
    }

    Scenario(Variant variant, Map<String, String> environment, PlannedOutage plannedOutage) {
        this.variant = variant;
        this.environment = environment;
        this.plannedOutage = plannedOutage;
    }

    public Optional<PlannedOutage> getPlannedOutage() {
        return Optional.ofNullable(plannedOutage);
    }

    // the crash point counts the warm-up requests too, e.g. afterWarmUp(50) crashes at the 50th load request
    private static String afterWarmUp(int loadRequest) {
        return String.valueOf(WarmUpService.WARM_UP_COUNT + loadRequest);
    }
}
