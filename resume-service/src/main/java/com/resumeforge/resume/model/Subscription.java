package com.resumeforge.resume.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "razorpay_order_id")
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id")
    private String razorpayPaymentId;

    @Column(name = "razorpay_signature")
    private String razorpaySignature;

    @Column(name = "plan", nullable = false)
    private String plan = "PRO";

    @Column(name = "amount_paise", nullable = false)
    private int amountPaise;

    @Column(name = "currency", nullable = false)
    private String currency = "INR";

    @Column(name = "status", nullable = false)
    private String status = "CREATED"; // CREATED | PAID | FAILED

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────

    public UUID getId()                               { return id; }
    public UUID getUserId()                           { return userId; }
    public void setUserId(UUID userId)                { this.userId = userId; }
    public String getRazorpayOrderId()                { return razorpayOrderId; }
    public void setRazorpayOrderId(String v)          { this.razorpayOrderId = v; }
    public String getRazorpayPaymentId()              { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String v)        { this.razorpayPaymentId = v; }
    public String getRazorpaySignature()              { return razorpaySignature; }
    public void setRazorpaySignature(String v)        { this.razorpaySignature = v; }
    public String getPlan()                           { return plan; }
    public void setPlan(String plan)                  { this.plan = plan; }
    public int getAmountPaise()                       { return amountPaise; }
    public void setAmountPaise(int v)                 { this.amountPaise = v; }
    public String getCurrency()                       { return currency; }
    public void setCurrency(String currency)          { this.currency = currency; }
    public String getStatus()                         { return status; }
    public void setStatus(String status)              { this.status = status; }
    public Instant getCreatedAt()                     { return createdAt; }
    public Instant getUpdatedAt()                     { return updatedAt; }
}
