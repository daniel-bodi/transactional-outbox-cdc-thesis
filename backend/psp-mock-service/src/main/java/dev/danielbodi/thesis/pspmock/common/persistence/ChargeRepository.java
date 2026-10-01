package dev.danielbodi.thesis.pspmock.common.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * @author danielbodi
 */
public interface ChargeRepository extends JpaRepository<Charge, UUID> {
}
