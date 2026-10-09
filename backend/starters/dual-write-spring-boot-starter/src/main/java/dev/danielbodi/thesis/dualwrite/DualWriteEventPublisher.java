package dev.danielbodi.thesis.dualwrite;

import dev.danielbodi.thesis.event.IntegrationEvent;
import dev.danielbodi.thesis.event.IntegrationEventPublisher;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author danielbodi
 */
public class DualWriteEventPublisher implements IntegrationEventPublisher {

    // the topic naming and the id header of the Debezium Outbox Event Router, so the consumers work unchanged
    private static final String TOPIC_PREFIX = "outbox.event.";
    private static final String EVENT_ID_HEADER = "id";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DualWriteEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(IntegrationEvent event) {
        // same contract as the outbox publisher: an event is only published as part of a business transaction
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Integration events can only be published within a transaction");
        }

        final String topic = TOPIC_PREFIX + event.getEventType();
        final String payload = objectMapper.writeValueAsString(event);

        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, event.getAggregateId(), payload);
        producerRecord.headers().add(EVENT_ID_HEADER, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send(producerRecord);
            }
        });
    }
}
