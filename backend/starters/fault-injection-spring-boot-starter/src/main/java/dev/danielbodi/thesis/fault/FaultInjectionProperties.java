package dev.danielbodi.thesis.fault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;

/**
 * Which crash point is armed for the current run (FAULT_ACTIVE_CRASH_POINT_NAME, FAULT_TARGET_REACH_COUNT).
 *
 * @param activeCrashPointName the armed crash point; no crash point is armed when empty
 * @param targetReachCount     the crash happens when the armed point is reached for the targetReachCount-th time
 * @param markerFile           written right before the crash, so the restarted container does not crash again
 * @author danielbodi
 */
@ConfigurationProperties("fault")
public record FaultInjectionProperties(String activeCrashPointName,
                                       @DefaultValue("1") int targetReachCount,
                                       @DefaultValue("/tmp/crash-point-fired") Path markerFile) {
}
