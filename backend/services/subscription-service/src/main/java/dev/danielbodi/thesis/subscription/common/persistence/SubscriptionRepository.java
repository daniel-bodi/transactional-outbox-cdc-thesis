package dev.danielbodi.thesis.subscription.common.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * @author danielbodi
 */
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
}
