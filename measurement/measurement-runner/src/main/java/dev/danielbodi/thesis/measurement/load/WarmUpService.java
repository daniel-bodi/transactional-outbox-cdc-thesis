package dev.danielbodi.thesis.measurement.load;

import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author danielbodi
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarmUpService {

    private final MeasurementProperties properties;
    private final LoadGenerator loadGenerator;

    public void warmUp(int warmUpCount, int ratePerSecond ) {
        log.info("Starting warm-up with [{}] requests and [{}] requests per second", warmUpCount, ratePerSecond);

        final List<RequestResult> warmUpResults = loadGenerator.generate(warmUpCount, ratePerSecond);

        final List<RequestResult> failed = warmUpResults.stream()
                .filter(request -> request.outcome() != RequestOutcome.CREATED)
                .toList();
        if (!failed.isEmpty()) {
            throw new IllegalStateException("Warm-up failed, the stack is not ready. Request details: " + failed);
        }

        log.info("Warm-up completed with [{}] subscriptions", warmUpResults.size());
    }
}
