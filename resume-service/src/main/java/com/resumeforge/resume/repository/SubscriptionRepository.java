package com.resumeforge.resume.repository;

import com.resumeforge.resume.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByRazorpayOrderId(String orderId);
}
