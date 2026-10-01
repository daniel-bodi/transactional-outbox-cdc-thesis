package dev.danielbodi.thesis.inbox.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.RecordInterceptor;

/**
 * @author danielbodi
 */
@RequiredArgsConstructor
public class EventIdRecordInterceptor implements RecordInterceptor<Object, Object> {

    private final String headerName;

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                    Consumer<Object, Object> consumer) {
        final Header header = record.headers().lastHeader(headerName);
        EventIdContext.set(consumer.groupMetadata().groupId(), headerName, header == null ? null : header.value());
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        EventIdContext.clear();
    }
}
