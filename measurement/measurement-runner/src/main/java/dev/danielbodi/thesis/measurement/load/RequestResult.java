package dev.danielbodi.thesis.measurement.load;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.Instant;
import java.util.UUID;

/**
 * @author danielbodi
 */
public record RequestResult(
        int index,
        UUID customerId,
        Instant sentAt,
        Instant finishedAt,
        RequestOutcome outcome,
        HttpStatus httpStatus,
        String error) {

    public static RequestResult created(int index, UUID customerId, Instant sentAt) {
        return new RequestResult(index, customerId, sentAt, Instant.now(), RequestOutcome.CREATED, HttpStatus.CREATED, null);
    }

    public static RequestResult rejected(int index, UUID customerId, Instant sentAt, HttpStatusCode httpStatus,
                                         String responseBody) {
        return new RequestResult(index, customerId, sentAt, Instant.now(), RequestOutcome.REJECTED,
                HttpStatus.valueOf(httpStatus.value()), responseBody);
    }

    public static RequestResult noResponse(int index, UUID customerId, Instant sentAt, String error) {
        return new RequestResult(index, customerId, sentAt, Instant.now(), RequestOutcome.NO_RESPONSE, null, error);
    }
}
