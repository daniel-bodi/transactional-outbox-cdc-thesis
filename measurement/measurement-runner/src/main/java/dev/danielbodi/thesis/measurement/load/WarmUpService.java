package dev.danielbodi.thesis.measurement.load;

import dev.danielbodi.thesis.measurement.evaluation.ConvergenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarmUpService {

    public static final int WARM_UP_COUNT = 10;

    private final LoadGenerator loadGenerator;
    private final ConvergenceService convergenceService;

    // the stack is ready when the warm-up subscriptions made the full round trip:
    // the services are up, the consumers have their partitions, the connectors run
    public void warmUp(int ratePerSecond, Duration timeout) {
        log.info("Starting warm-up with [{}] requests and [{}] requests per second", WARM_UP_COUNT, ratePerSecond);

        final List<RequestResult> warmUpResults = loadGenerator.generate(WARM_UP_COUNT, ratePerSecond);

        final List<RequestResult> failed = warmUpResults.stream()
                .filter(request -> request.outcome() != RequestOutcome.CREATED)
                .toList();
        if (!failed.isEmpty()) {
            throw new IllegalStateException("Warm-up failed, the stack is not ready. Request details: " + failed);
        }

        if (!convergenceService.awaitNoPending(timeout)) {
            throw new IllegalStateException("Warm-up subscriptions did not leave PENDING within " + timeout);
        }

        log.info("Warm-up completed with [{}] subscriptions", warmUpResults.size());
    }
}
