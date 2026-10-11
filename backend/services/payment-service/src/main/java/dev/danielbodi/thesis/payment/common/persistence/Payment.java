package dev.danielbodi.thesis.payment.common.persistence;

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
@Table(name = "payment")
public class Payment {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID reference;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "provider_reference")
    private String providerReference;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    private Payment(UUID reference, BigDecimal amount, PaymentStatus status, String providerReference) {
        this.reference = reference;
        this.amount = amount;
        this.status = status;
        this.providerReference = providerReference;
    }

    public static Payment succeeded(UUID reference, BigDecimal amount, String providerReference) {
        return new Payment(reference, amount, PaymentStatus.SUCCEEDED, providerReference);
    }

    public static Payment failed(UUID reference, BigDecimal amount, String providerReference) {
        return new Payment(reference, amount, PaymentStatus.FAILED, providerReference);
    }
}
