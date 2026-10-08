package dev.danielbodi.thesis.pspmock;

import dev.danielbodi.thesis.pspmock.charge.ChargeRequest;
import dev.danielbodi.thesis.pspmock.charge.ChargeResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author danielbodi
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "psp.idempotency-key.enabled=true")
class IdempotencyKeysEnabledIntegrationTest {

    @TestConfiguration
    static class ContainerConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:18.6");
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void duplicateRequestWithSameIdempotencyKeyIsNotChargedAgain() {
        final UUID reference = UUID.randomUUID();
        final String idempotencyKey = UUID.randomUUID().toString();

        final ChargeResponse original = charge(reference, idempotencyKey);
        final ChargeResponse duplicate = charge(reference, idempotencyKey);

        assertSoftly(softly -> {
            softly.assertThat(duplicate).isEqualTo(original);
            softly.assertThat(chargeCount(reference)).isEqualTo(1);
            softly.assertThat(duplicateRequests(reference)).isEqualTo(1);
        });
    }

    @Test
    void requestWithoutIdempotencyKeyIsRejected() {
        final UUID reference = UUID.randomUUID();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> charge(reference, null))
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
            softly.assertThat(chargeCount(reference)).isZero();
        });
    }

    @Test
    void requestWithBlankIdempotencyKeyIsRejected() {
        final UUID reference = UUID.randomUUID();

        assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> charge(reference, " "))
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
            softly.assertThat(chargeCount(reference)).isZero();
        });
    }

    private ChargeResponse charge(UUID reference, String idempotencyKey) {
        final String baseUrl = "http://localhost:%d".formatted(port);
        final RestClient.RequestBodySpec request = RestClient.create(baseUrl)
                .post()
                .uri("/charges")
                .contentType(MediaType.APPLICATION_JSON);

        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }

        return request.body(new ChargeRequest(reference, new BigDecimal("999.99")))
                .retrieve()
                .body(ChargeResponse.class);
    }

    private int chargeCount(UUID reference) {
        return jdbcClient.sql("SELECT count(*) FROM charge WHERE reference = ?")
                .param(reference)
                .query(Integer.class)
                .single();
    }

    private int duplicateRequests(UUID reference) {
        return jdbcClient.sql("SELECT duplicate_requests FROM charge WHERE reference = ?")
                .param(reference)
                .query(Integer.class)
                .single();
    }
}
