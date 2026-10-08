package dev.danielbodi.thesis.fault;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author danielbodi
 */
@AutoConfiguration
@EnableConfigurationProperties(FaultInjectionProperties.class)
public class FaultInjectionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    FaultInjector faultInjector(FaultInjectionProperties properties) {
        return new FaultInjector(properties.activeCrashPointName(), properties.targetReachCount(), properties.markerFile(),
                // Runtime.halt forcibly terminate the running JVM
                // neither the shutdown hooks nor the finalizers are executed
                Runtime.getRuntime()::halt);
    }
}
