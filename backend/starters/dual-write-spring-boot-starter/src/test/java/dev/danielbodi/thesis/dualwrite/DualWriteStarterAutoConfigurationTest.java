package dev.danielbodi.thesis.dualwrite;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author danielbodi
 */
class DualWriteStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DualWriteAutoConfiguration.class))
            .withBean(ObjectMapper.class, () -> JsonMapper.builder().build())
            .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class));

    @Test
    void publisherIsNotCreatedByDefault() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(DualWriteEventPublisher.class));
    }

    @Test
    void publisherIsCreatedWhenEnabled() {
        contextRunner
                .withPropertyValues("dual-write.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(DualWriteEventPublisher.class));
    }
}
