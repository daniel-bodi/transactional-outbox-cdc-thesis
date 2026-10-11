package dev.danielbodi.thesis.measurement.load;

import dev.danielbodi.thesis.measurement.client.SubscriptionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoadGenerator {

    private final SubscriptionServiceClient subscriptionServiceClient;

    // sends the requests at a fixed rate, e.g. 10 per second = one request every 100 ms
    public List<RequestResult> generate(int requestCount, int ratePerSecond) {
        log.info("Starting load generation with [{}] requests and [{}] requests per second", requestCount, ratePerSecond);

        final Duration interval = Duration.ofSeconds(1).dividedBy(ratePerSecond);
        final List<Future<RequestResult>> futures = new ArrayList<>(requestCount);

        // every request gets its own virtual thread, so a slow or hanging request
        // (e.g. while the service is down) does not hold back the ones after it
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            // use monotonic clock for scheduling - unlike the wall clock, it never jumps (like on ntp time sync)
            final long start = System.nanoTime();

            for (int i = 0; i < requestCount; i++) {
                // the sleeping time is counted from the start of the load and not from the previous request,
                // so if a previous sleep wakes up late, the next one is shorter, hence the delays are compensated
                sleepUntil(start + interval.multipliedBy(i).toNanos());

                final int index = i + 1;
                futures.add(executor.submit(() -> send(index)));
            }
        } // closing the ExecutorService waits until every request has finished or times out

        final List<RequestResult> requestResultList = futures.stream().map(LoadGenerator::result).toList();

        log.info("Load generation completed with [{}] requests", requestResultList.size());

        return requestResultList;
    }

    private RequestResult send(int index) {
        final UUID customerId = UUID.randomUUID();
        final Instant sentAt = Instant.now();

        try {
            final HttpStatusCode status = subscriptionServiceClient.createSubscription(customerId);
            return status.isSameCodeAs(HttpStatus.CREATED)
                    ? RequestResult.created(index, customerId, sentAt)
                    : RequestResult.rejected(index, customerId, sentAt, status, null);
        } catch (RestClientResponseException e) {
            // 4xx or 5xx response
            return RequestResult.rejected(index, customerId, sentAt, e.getStatusCode(), e.getResponseBodyAsString());

        } catch (ResourceAccessException e) {
            // no response: connection refused or reset, timeout
            return RequestResult.noResponse(index, customerId, sentAt, e.getMessage());
        }
    }

    private static void sleepUntil(long targetNanoTime) {
        final long remaining = targetNanoTime - System.nanoTime();

        if (remaining > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private static RequestResult result(Future<RequestResult> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            throw new IllegalStateException(e.getCause());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
