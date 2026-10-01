package dev.danielbodi.thesis.inbox.kafka;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author danielbodi
 */
public final class EventIdContext {

    private static final ThreadLocal<CapturedRecord> CURRENT = new ThreadLocal<>();

    private EventIdContext() {
    }

    static void set(String consumerGroup, String headerName, byte[] value) {
        CURRENT.set(new CapturedRecord(consumerGroup, headerName, value));
    }

    static void clear() {
        CURRENT.remove();
    }

    public static String currentConsumerGroup() {
        return captured().consumerGroup();
    }

    public static UUID currentEventId() {
        final CapturedRecord captured = captured();
        if (captured.value() == null) {
            throw new IllegalStateException(
                    "Consumer record has no [" + captured.headerName() + "] header");
        }
        final String raw = new String(captured.value(), StandardCharsets.UTF_8);
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Header [" + captured.headerName() + "] is not a UUID: [" + raw + "]", e);
        }
    }

    private static CapturedRecord captured() {
        final CapturedRecord captured = CURRENT.get();
        if (captured == null) {
            throw new IllegalStateException(
                    "No consumer record in context: @Idempotent method was not invoked by a Kafka listener container "
                    + "with EventIdRecordInterceptor registered");
        }
        return captured;
    }

    private record CapturedRecord(String consumerGroup, String headerName, byte[] value) {
    }
}
