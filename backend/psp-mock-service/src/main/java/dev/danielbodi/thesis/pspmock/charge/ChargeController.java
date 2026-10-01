package dev.danielbodi.thesis.pspmock.charge;

import dev.danielbodi.thesis.pspmock.charge.service.ChargeService;
import dev.danielbodi.thesis.pspmock.common.persistence.Charge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author danielbodi
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/charges")
public class ChargeController {

    private final ChargeService chargeService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ChargeResponse charge(@RequestBody ChargeRequest request) {
        log.info("Charge requested for reference: [{}] with amount: [{}]", request.reference(), request.amount());

        final Charge charge = chargeService.charge(request.reference(), request.amount());

        log.info("Charge [{}] executed for reference: [{}] with status: [{}]",
                charge.getId(), charge.getReference(), charge.getStatus());

        return new ChargeResponse(charge.getId(), charge.getStatus());
    }
}
