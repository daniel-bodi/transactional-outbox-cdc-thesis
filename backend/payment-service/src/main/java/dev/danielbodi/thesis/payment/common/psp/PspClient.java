package dev.danielbodi.thesis.payment.common.psp;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @author danielbodi
 */
@Component
@RequiredArgsConstructor
public class PspClient {

    private final RestClient pspRestClient;

    public PspChargeResponse charge(UUID reference, BigDecimal amount) {
        return pspRestClient.post()
                .uri("/charges")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PspChargeRequest(reference, amount))
                .retrieve()
                .body(PspChargeResponse.class);
    }
}
