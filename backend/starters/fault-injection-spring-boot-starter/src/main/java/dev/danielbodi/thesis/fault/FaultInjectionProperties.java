package dev.danielbodi.thesis.fault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

/**
 * @author danielbodi
 */
@ConfigurationProperties("fault")
public record FaultInjectionProperties(String activeCrashPointName,
                                       @DefaultValue("1") int targetReachCount,
                                       @DefaultValue("/tmp/crash-point-fired") Path markerFile) {

    public FaultInjectionProperties {
        if (targetReachCount < 1) {
            throw new IllegalArgumentException(
                    "Target reach count must be at least 1, but was [%d]".formatted(targetReachCount));
        }
    }
}
