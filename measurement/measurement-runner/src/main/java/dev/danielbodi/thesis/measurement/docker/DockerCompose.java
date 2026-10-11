package dev.danielbodi.thesis.measurement.docker;

import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

/**
 * @author danielbodi
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DockerCompose {

    private final MeasurementProperties properties;

    public void up(Map<String, String> environment) {
        run(environment, "docker", "compose", "up", "--detach", "--no-build", "--wait", "--wait-timeout", "300");
    }

    public void down() {
        run(Map.of(), "docker", "compose", "down", "--volumes", "--remove-orphans");
    }

    public void registerConnectors() {
        final Map<String, String> environment = Map.of("KAFKA_CONNECT_URL", properties.kafkaConnectUrl());
        run(environment, "./infrastructure/debezium/register-connectors.sh");
    }

    private void run(Map<String, String> environment, String... command) {
        final Path composeDirectory = properties.composeDirectory().toAbsolutePath().normalize();

        final String fullCommand = String.join(" ", command);
        log.info("$ {}", fullCommand);

        try {
            final ProcessBuilder builder = new ProcessBuilder(command)
                    .directory(composeDirectory.toFile())
                    .redirectErrorStream(true);
            builder.environment().putAll(environment);

            final Process process = builder.start();
            final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            final int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Command failed with exit code [%d]: %s%n%s".formatted(exitCode, fullCommand, output));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running: [%s]".formatted(fullCommand), e);
        }
    }
}
