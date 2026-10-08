package dev.danielbodi.thesis.pspmock.charge;

import dev.danielbodi.thesis.pspmock.charge.service.ChargeService;
import dev.danielbodi.thesis.pspmock.common.persistence.Charge;
import dev.danielbodi.thesis.pspmock.configuration.IdempotencyKeyProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author danielbodi
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/charges")
public class ChargeController {

    private final ChargeService chargeService;
    private final IdempotencyKeyProperties idempotencyKeyProperties;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChargeResponse charge(@RequestBody ChargeRequest request,
                                 @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        log.info("Charge requested for reference: [{}] with amount: [{}] and idempotency key: [{}]",
                request.reference(), request.amount(), idempotencyKey);

        // with idempotency key handling enabled, the header is mandatory
        if (idempotencyKeyProperties.enabled() && !StringUtils.hasText(idempotencyKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required");
        }

        final Charge charge = chargeService.charge(request.reference(), request.amount(), idempotencyKey);

        return new ChargeResponse(charge.getId(), charge.getStatus());
    }
}
