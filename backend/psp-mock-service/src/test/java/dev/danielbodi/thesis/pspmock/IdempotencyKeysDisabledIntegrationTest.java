package dev.danielbodi.thesis.pspmock;

import dev.danielbodi.thesis.pspmock.charge.ChargeRequest;
import dev.danielbodi.thesis.pspmock.charge.ChargeResponse;
import dev.danielbodi.thesis.pspmock.common.persistence.ChargeStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author danielbodi
 */
// a provider without idempotency support
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "psp.idempotency-key.enabled=false")
class IdempotencyKeysDisabledIntegrationTest {

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
    void chargeIsExecutedAndRecorded() {
        final UUID reference = UUID.randomUUID();

        final ResponseEntity<ChargeResponse> response = charge(reference, null);

        assertSoftly(softly -> {
            softly.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            softly.assertThat(response.getBody().status()).isEqualTo(ChargeStatus.SUCCEEDED);
            softly.assertThat(chargeCount(reference)).isEqualTo(1);
        });
    }

    @Test
    void repeatedIdempotencyKeyIsChargedAgainWhenKeysAreIgnored() {
        final UUID reference = UUID.randomUUID();
        final String idempotencyKey = UUID.randomUUID().toString();

        charge(reference, idempotencyKey);
        charge(reference, idempotencyKey);

        assertSoftly(softly -> softly.assertThat(chargeCount(reference)).isEqualTo(2));
    }

    private ResponseEntity<ChargeResponse> charge(UUID reference, String idempotencyKey) {
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
                .toEntity(ChargeResponse.class);
    }

    private int chargeCount(UUID reference) {
        return jdbcClient.sql("SELECT count(*) FROM charge WHERE reference = ?")
                .param(reference)
                .query(Integer.class)
                .single();
    }
}
