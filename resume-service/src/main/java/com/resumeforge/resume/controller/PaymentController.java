package com.resumeforge.resume.controller;

import com.resumeforge.resume.service.PaymentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:3000"})
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/v1/payments/create-order
     * Creates a Razorpay order for the PRO plan.
     * Requires valid JWT.
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(paymentService.createOrder(userId));
    }

    /**
     * POST /api/v1/payments/verify
     * Verifies Razorpay payment signature and upgrades user to PRO.
     * Body: { razorpayOrderId, razorpayPaymentId, razorpaySignature }
     */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(
            @AuthenticationPrincipal UUID userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(paymentService.verifyPayment(userId, body));
    }

    /**
     * POST /api/v1/payments/webhook
     * Razorpay server-to-server webhook (no JWT required, but signature-verified).
     * Register this URL in Razorpay Dashboard → Webhooks.
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> webhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        paymentService.handleWebhook(rawBody, signature != null ? signature : "");
        return ResponseEntity.ok().build();
    }
}
