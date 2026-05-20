package com.resumeforge.resume.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.resumeforge.resume.model.Subscription;
import com.resumeforge.resume.model.User;
import com.resumeforge.resume.repository.SubscriptionRepository;
import com.resumeforge.resume.repository.UserRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // PRO plan: ₹499/month in paise
    public static final int PRO_AMOUNT_PAISE = 49900;
    public static final String CURRENCY = "INR";

    private final SubscriptionRepository subscriptionRepo;
    private final UserRepository userRepo;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    public PaymentService(SubscriptionRepository subscriptionRepo, UserRepository userRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.userRepo         = userRepo;
    }

    /**
     * Create a Razorpay order for PRO subscription.
     * Returns { orderId, amount, currency, keyId }
     */
    @Transactional
    public Map<String, Object> createOrder(UUID userId) {
        validateRazorpayConfig();

        try {
            RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject options = new JSONObject();
            options.put("amount", PRO_AMOUNT_PAISE);
            options.put("currency", CURRENCY);
            options.put("receipt", "rf_" + userId.toString().replace("-", "").substring(0, 12));
            options.put("notes", new JSONObject().put("userId", userId.toString()));

            Order order = client.orders.create(options);
            String orderId = order.get("id");

            // Persist subscription record
            Subscription sub = new Subscription();
            sub.setUserId(userId);
            sub.setRazorpayOrderId(orderId);
            sub.setAmountPaise(PRO_AMOUNT_PAISE);
            sub.setCurrency(CURRENCY);
            sub.setStatus("CREATED");
            subscriptionRepo.save(sub);

            log.info("[PAYMENT] Created Razorpay order={} for userId={}", orderId, userId);

            return Map.of(
                "orderId",   orderId,
                "amount",    PRO_AMOUNT_PAISE,
                "currency",  CURRENCY,
                "keyId",     razorpayKeyId
            );
        } catch (RazorpayException e) {
            log.error("[PAYMENT] Razorpay order creation failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Payment gateway error. Please try again.");
        }
    }

    /**
     * Verify Razorpay payment signature and upgrade user to PRO.
     * body = { razorpayOrderId, razorpayPaymentId, razorpaySignature }
     */
    @Transactional
    public Map<String, String> verifyPayment(UUID userId, Map<String, String> body) {
        validateRazorpayConfig();

        String orderId   = body.get("razorpayOrderId");
        String paymentId = body.get("razorpayPaymentId");
        String signature = body.get("razorpaySignature");

        if (orderId == null || paymentId == null || signature == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Missing payment verification fields.");
        }

        // Verify HMAC-SHA256 signature: orderId + "|" + paymentId
        try {
            JSONObject params = new JSONObject();
            params.put("razorpay_order_id",   orderId);
            params.put("razorpay_payment_id", paymentId);
            params.put("razorpay_signature",  signature);

            boolean valid = Utils.verifyPaymentSignature(params, razorpayKeySecret);
            if (!valid) {
                log.warn("[PAYMENT] Invalid signature for orderId={}", orderId);
                throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                        "Payment signature verification failed.");
            }
        } catch (RazorpayException e) {
            log.error("[PAYMENT] Signature verification error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Signature verification error.");
        }

        // Update subscription record
        Subscription sub = subscriptionRepo.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found."));

        sub.setRazorpayPaymentId(paymentId);
        sub.setRazorpaySignature(signature);
        sub.setStatus("PAID");
        subscriptionRepo.save(sub);

        // Upgrade user to PRO
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPlan("PRO");
        userRepo.save(user);

        log.info("[PAYMENT] Payment verified, upgraded userId={} to PRO", userId);
        return Map.of("status", "success", "plan", "PRO");
    }

    /**
     * Handle Razorpay webhook.
     * Verifies X-Razorpay-Signature header (HMAC-SHA256 of raw body with webhook secret).
     * Idempotent: safe to call multiple times with the same payload.
     */
    @Transactional
    public void handleWebhook(String rawBody, String razorpaySignature) {
        validateRazorpayConfig();

        // Verify webhook signature: HMAC-SHA256(rawBody, webhookSecret)
        // Razorpay webhook secret is separate from the API secret; default to keySecret for dev
        String webhookSecret = razorpayKeySecret; // replace with ${razorpay.webhook-secret} in prod

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hmac = mac.doFinal(rawBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) sb.append(String.format("%02x", b));
            String computed = sb.toString();

            if (!computed.equals(razorpaySignature)) {
                log.warn("[WEBHOOK] Invalid Razorpay signature. Rejecting.");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature.");
            }
        } catch (ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            log.error("[WEBHOOK] Signature verification error: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Webhook signature error.");
        }

        // Parse event
        try {
            org.json.JSONObject event = new org.json.JSONObject(rawBody);
            String eventType = event.optString("event", "");
            log.info("[WEBHOOK] Received event type={}", eventType);

            if ("payment.captured".equals(eventType)) {
                org.json.JSONObject paymentEntity = event
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String orderId   = paymentEntity.optString("order_id");
                String paymentId = paymentEntity.optString("id");

                subscriptionRepo.findByRazorpayOrderId(orderId).ifPresent(sub -> {
                    if (!"PAID".equals(sub.getStatus())) {
                        sub.setRazorpayPaymentId(paymentId);
                        sub.setStatus("PAID");
                        subscriptionRepo.save(sub);
                        userRepo.findById(sub.getUserId()).ifPresent(u -> {
                            u.setPlan("PRO");
                            userRepo.save(u);
                            log.info("[WEBHOOK] Upgraded userId={} to PRO via webhook", u.getId());
                        });
                    }
                });
            }
        } catch (Exception e) {
            log.error("[WEBHOOK] Event parsing error: {}", e.getMessage());
            // Don't rethrow — return 200 so Razorpay doesn't retry
        }
    }

    private void validateRazorpayConfig() {
        if (razorpayKeyId.contains("placeholder") || razorpayKeySecret.contains("placeholder")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment gateway not configured. Please contact support.");
        }
    }
}
