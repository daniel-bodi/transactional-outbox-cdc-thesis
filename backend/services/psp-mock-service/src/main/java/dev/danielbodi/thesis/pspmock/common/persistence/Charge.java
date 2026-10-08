package dev.danielbodi.thesis.pspmock.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author danielbodi
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "charge")
public class Charge {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false)
    private UUID reference;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "duplicate_requests", nullable = false)
    private int duplicateRequests;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    private Charge(UUID reference, BigDecimal amount, ChargeStatus status, String idempotencyKey) {
        this.reference = reference;
        this.amount = amount;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
    }

    public static Charge withIdempotencyKey(UUID reference, BigDecimal amount, ChargeStatus status, String idempotencyKey) {
        return new Charge(reference, amount, status, idempotencyKey);
    }

    public static Charge withoutIdempotencyKey(UUID reference, BigDecimal amount, ChargeStatus status) {
        return new Charge(reference, amount, status, null);
    }

    public void incrementDuplicateRequests() {
        duplicateRequests++;
    }
}
