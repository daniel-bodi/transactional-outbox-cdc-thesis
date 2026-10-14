package dev.danielbodi.thesis.measurement.docker;

import dev.danielbodi.thesis.measurement.properties.MeasurementProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author danielbodi
 */
@Component
@RequiredArgsConstructor
public class ContainerEventRecorder {

    private static final Set<String> ACTIONS = Set.of("start", "die", "health_status: healthy", "health_status: unhealthy");

    private final MeasurementProperties properties;
    private final JsonMapper jsonMapper;
    private final List<ContainerEvent> events = new CopyOnWriteArrayList<>();

    private Process process;
    private Thread readerThread;

    public void start() {
        events.clear();
        try {
            process = new ProcessBuilder("docker", "compose", "events", "--json")
                    .directory(properties.composeDirectory().toAbsolutePath().normalize().toFile())
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        readerThread = Thread.ofVirtual().start(this::read);
    }

    public void stop() {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            readerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        process = null;
    }

    public List<ContainerEvent> events() {
        return List.copyOf(events);
    }

    private void read() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                final JsonNode event = jsonMapper.readTree(line);
                final String action = event.path("action").asString();
                if (ACTIONS.contains(action)) {
                    final JsonNode attributes = event.path("attributes");
                    events.add(new ContainerEvent(
                            // e.g. 2026-10-14T07:38:21.659482794+02:00, nanosecond precision
                            Instant.parse(event.path("time").asString()),
                            attributes.path("name").asString(),
                            action,
                            // exitCode is only set on die events
                            attributes.has("exitCode") ? Integer.valueOf(attributes.get("exitCode").asString()) : null));
                }
            }
        } catch (IOException _) {
            // the expected end of the reading: stop() destroys the process,
            // which closes the input stream under the blocked readLine()
        }
    }
}
