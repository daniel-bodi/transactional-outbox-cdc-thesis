package dev.danielbodi.thesis.idempotency.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * @author danielbodi
 */
@RequiredArgsConstructor
public class ConsumedRecordInterceptor implements RecordInterceptor<Object, Object> {

    private final String headerName;

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                    Consumer<Object, Object> consumer) {
        final Header header = record.headers().lastHeader(headerName);
        ConsumedRecordContext.set(consumer.groupMetadata().groupId(), headerName, header == null ? null : header.value());
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        ConsumedRecordContext.clear();
    }
}
