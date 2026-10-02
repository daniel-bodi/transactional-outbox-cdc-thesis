package dev.danielbodi.thesis.payment.common.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * @author danielbodi
 */
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
