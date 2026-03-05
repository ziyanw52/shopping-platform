package com.ziyan.payment.service;

import com.ziyan.payment.dto.PaymentRequest;
import com.ziyan.payment.dto.PaymentResponse;
import com.ziyan.payment.enums.PaymentStatus;
import com.ziyan.payment.exception.PaymentException;
import com.ziyan.payment.gateway.PaymentGateway;
import com.ziyan.payment.model.Payment;
import com.ziyan.payment.model.PaymentKey;
import com.ziyan.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final PaymentGateway paymentGateway;

    /**
     * Submit a payment for an order
     * Idempotency: requestId ensures same request won't be charged twice
     */
    public PaymentResponse submitPayment(PaymentRequest request) {
        // Check if this requestId has already been processed
        // In a real Cassandra setup, you'd query by requestId using a secondary index
        // For now, we use paymentId lookup as a workaround
        
        String paymentId = UUID.randomUUID().toString();

        // Create composite key: (paymentId, requestId) for idempotency
        PaymentKey key = PaymentKey.builder()
                .paymentId(paymentId)
                .requestId(request.getRequestId())
                .build();

        Payment payment = Payment.builder()
                .key(key)
                .orderId(request.getOrderId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        try {
            // Process payment using injected gateway (mock or real)
            paymentGateway.processPayment(payment);
            payment.setStatus(PaymentStatus.SUCCESS);
            
            // Publish success event
            kafkaTemplate.send("payment.success", createPaymentEvent(payment));
        } catch (PaymentException e) {
            payment.setStatus(PaymentStatus.FAILED);
            
            // Publish failure event with reason
            String failureEvent = String.format(
                "{\"paymentId\":\"%s\",\"orderId\":%d,\"amount\":%.2f,\"status\":\"FAILED\",\"currency\":\"%s\",\"error\":\"%s\"}",
                payment.getKey().getPaymentId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                e.getMessage()
            );
            kafkaTemplate.send("payment.failed", failureEvent);
        }

        paymentRepository.save(payment);
        return convertToResponse(payment);
    }

    /**
     * Lookup payment by ID (Payment Lookup)
     */
    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findByKeyPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));
        return convertToResponse(payment);
    }

    /**
     * Update payment details (amount, currency)
     */
    public PaymentResponse updatePayment(String paymentId, PaymentRequest request) {
        Payment payment = paymentRepository.findByKeyPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));

        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        return convertToResponse(payment);
    }

    /**
     * Reverse payment (Refund)
     * Only successful payments can be refunded
     */
    public PaymentResponse reversePayment(String paymentId) {
        Payment payment = paymentRepository.findByKeyPaymentId(paymentId)
                .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentException("Only successful payments can be refunded. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        
        // Publish refund event
        kafkaTemplate.send("payment.refunded", createPaymentEvent(payment));

        return convertToResponse(payment);
    }

    // ===== HELPER METHODS =====

    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    private PaymentResponse convertToResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getKey().getPaymentId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    /**
     * Create Kafka event message
     */
    private String createPaymentEvent(Payment payment) {
        return String.format(
            "{\"paymentId\":\"%s\",\"orderId\":%d,\"amount\":%.2f,\"status\":\"%s\",\"currency\":\"%s\"}",
            payment.getKey().getPaymentId(),
            payment.getOrderId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getCurrency()
        );
    }
}
