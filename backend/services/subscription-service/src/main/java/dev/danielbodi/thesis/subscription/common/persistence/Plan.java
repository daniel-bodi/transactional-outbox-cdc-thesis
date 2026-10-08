package dev.danielbodi.thesis.subscription.common.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Reference data owned by the subscription bounded context.
 * <p>
 * The plan is the source of truth for the price a subscription is billed at;
 * repricing a plan therefore reprices every subscription that references it.
 *
 * @author danielbodi
 */
@Entity
@Table(name = "plan")
@Getter
@NoArgsConstructor
public class Plan {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal amount;
}
