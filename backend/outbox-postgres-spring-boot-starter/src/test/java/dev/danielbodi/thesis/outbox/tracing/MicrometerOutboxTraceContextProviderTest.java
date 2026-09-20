package dev.danielbodi.thesis.outbox.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static dev.danielbodi.thesis.outbox.tracing.MicrometerOutboxTraceContextProvider.B3_HEADER;
import static dev.danielbodi.thesis.outbox.tracing.MicrometerOutboxTraceContextProvider.W3C_TRACEPARENT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author danielbodi
 */
class MicrometerOutboxTraceContextProviderTest {

    private final Tracer tracer = mock(Tracer.class);
    private final Propagator propagator = mock(Propagator.class);
    private final MicrometerOutboxTraceContextProvider provider =
            new MicrometerOutboxTraceContextProvider(tracer, propagator);

    @Test
    void traceContextProviderReturnsNullWhenNoCurrentSpan() {
        when(tracer.currentSpan()).thenReturn(null);

        assertThat(provider.currentTraceContext()).isNull();
    }

    @Test
    void traceContextProviderReturnsWhateverThePropagatorPutsUnderTheTraceparentKey() {
        givenTheCurrentSpanContext();

        final String traceId = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
        givenThePropagatorInjectsHeaders(W3C_TRACEPARENT_HEADER, traceId);

        assertThat(provider.currentTraceContext()).isEqualTo(traceId);
    }

    @Test
    void traceContextProviderFallsBackToB3WhenPropagatorEmitsB3HeadersInsteadOfTraceparent() {
        givenTheCurrentSpanContext();

        final String traceId = "0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-1";
        givenThePropagatorInjectsHeaders(B3_HEADER, traceId);

        assertThat(provider.currentTraceContext()).isEqualTo(traceId);
    }

    @Test
    void traceContextProviderReturnsNullWhenPropagatorEmitsCustomHeader() {
        givenTheCurrentSpanContext();
        givenThePropagatorInjectsHeaders("x-custom-trace", "irrelevant");

        assertThat(provider.currentTraceContext()).isNull();
    }

    private void givenTheCurrentSpanContext() {
        final Span span = mock(Span.class);
        final TraceContext context = mock(TraceContext.class);
        when(span.context()).thenReturn(context);
        when(tracer.currentSpan()).thenReturn(span);
    }

    // Propagator.inject is void and works by side effect — it writes headers into the carrier the caller passed in.
    // doAnswer lets the mock do the same, this behavior is mandatory because the context provider relies on it.
    @SuppressWarnings("unchecked")
    private void givenThePropagatorInjectsHeaders(String headerKey, String headerValue) {
        doAnswer(invocation -> {
            final Map<String, String> carrier = invocation.getArgument(1);
            final Propagator.Setter<Map<String, String>> setter = invocation.getArgument(2);
            setter.set(carrier, headerKey, headerValue);

            return null;
        }).when(propagator).inject(any(TraceContext.class), any(Map.class), any(Propagator.Setter.class));
    }
}
