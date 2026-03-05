package com.ziyan.payment.controller;

import com.ziyan.payment.dto.PaymentRequest;
import com.ziyan.payment.dto.PaymentResponse;
import com.ziyan.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment REST API Controller
 * 4 Endpoints:
 * 1. POST /payments - Submit payment
 * 2. GET /payments/{paymentId} - Lookup payment
 * 3. PUT /payments/{paymentId} - Update payment
 * 4. POST /payments/{paymentId}/refund - Reverse/refund payment
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Submit Payment
     * POST /payments
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> submitPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.submitPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Payment Lookup
     * GET /payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update Payment
     * PUT /payments/{paymentId}
     */
    @PutMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> updatePayment(
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.updatePayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Reverse Payment (Refund)
     * POST /payments/{paymentId}/refund
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<PaymentResponse> reversePayment(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.reversePayment(paymentId);
        return ResponseEntity.ok(response);
    }
}
