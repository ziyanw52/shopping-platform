package com.ziyan.payment.service;

import com.ziyan.payment.client.ItemDetailDto;
import com.ziyan.payment.client.ItemServiceClient;
import com.ziyan.payment.client.OrderDetailDto;
import com.ziyan.payment.client.OrderServiceClient;
import com.ziyan.payment.dto.PaymentRequest;
import com.ziyan.payment.dto.PaymentResponse;
import com.ziyan.payment.enums.PaymentStatus;
import com.ziyan.payment.event.PaymentEvent;
import com.ziyan.payment.exception.PaymentException;
import com.ziyan.payment.gateway.PaymentGateway;
import com.ziyan.payment.model.Payment;
import com.ziyan.payment.model.PaymentKey;
import com.ziyan.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final ItemServiceClient itemServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final PaymentEventProducer eventProducer;

    /**
     * Submit a payment for an order
     * Flow: Check idempotency → Reserve stock → Process payment → Publish event → Confirm/Restock
     * Idempotency: requestId ensures same request won't be charged twice
     * Order uniqueness: Each order can only be paid once
     */
    public PaymentResponse submitPayment(PaymentRequest request) {

        // 0. Check idempotency - if requestId already exists, return existing payment
        Payment existingPayment = paymentRepository.findByKeyRequestId(request.getRequestId())
                .orElse(null);
        
        if (existingPayment != null) {
            log.info("Duplicate request detected: requestId={}, returning existing payment={}", 
                    request.getRequestId(), existingPayment.getKey().getPaymentId());
            return convertToResponse(existingPayment);
        }

        // 0.5. Check if order already has a payment (prevent double payment for same order)
        Payment existingOrderPayment = paymentRepository.findByOrderId(request.getOrderId())
                .orElse(null);
        
        if (existingOrderPayment != null) {
            log.warn("Order {} already has a payment: paymentId={}", 
                    request.getOrderId(), existingOrderPayment.getKey().getPaymentId());
            throw new PaymentException("Order " + request.getOrderId() + " has already been paid. Payment ID: " 
                    + existingOrderPayment.getKey().getPaymentId());
        }

        // 1. Get order details to know which item/quantity to reserve
        OrderDetailDto order;
        try {
            order = orderServiceClient.getOrderDetails(request.getOrderId());
        } catch (Exception e) {
            log.error("Failed to fetch order details for order {}: {}", request.getOrderId(), e.getMessage());
            throw new PaymentException("Order not found or order service unavailable: " + request.getOrderId());
        }

        // 2. Validate payment amount matches item price × quantity
        ItemDetailDto item;
        try {
            item = itemServiceClient.getItem(order.getItemId());
            BigDecimal expectedAmount = BigDecimal.valueOf(item.getPrice())
                    .multiply(BigDecimal.valueOf(order.getQuantity()));
            // Allow small floating point tolerance (1 cent)
            if (request.getAmount().subtract(expectedAmount).abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
                log.error("Amount mismatch: paid {} but expected {} (price {} x qty {})",
                        request.getAmount(), expectedAmount, item.getPrice(), order.getQuantity());
                throw new PaymentException(
                        String.format("Invalid amount: paid %s but order total is %s (%s × %d)",
                                request.getAmount(), expectedAmount, item.getPrice(), order.getQuantity()));
            }
            log.info("Amount validated: {} matches expected {}", request.getAmount(), expectedAmount);
        } catch (PaymentException e) {
            throw e; // re-throw validation errors
        } catch (Exception e) {
            log.error("Failed to validate amount for item {}: {}", order.getItemId(), e.getMessage());
            throw new PaymentException("Unable to validate payment amount — item service unavailable");
        }

        // 3. Reserve stock BEFORE processing payment
        try {
            log.info("Reserving stock for item {} quantity {}", order.getItemId(), order.getQuantity());
            itemServiceClient.reserveItem(order.getItemId(), order.getQuantity());
            log.info("Stock reserved successfully");
        } catch (Exception e) {
            log.error("Insufficient stock for item {}: {}", order.getItemId(), e.getMessage());
            // Mark order as payment failed due to stock
            try {
                orderServiceClient.markOrderPaymentFailed(request.getOrderId());
            } catch (Exception ex) {
                log.error("Failed to mark order payment failed: {}", ex.getMessage());
            }
            throw new PaymentException("Not enough stock for item: " + order.getItemId());
        }

        // 4. Create payment record
        String paymentId = UUID.randomUUID().toString();

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

        // 5. Process payment
        String failureReason = null;
        try {
            paymentGateway.processPayment(payment);
            payment.setStatus(PaymentStatus.SUCCESS);
            log.info("Payment processed successfully: paymentId={}", payment.getKey().getPaymentId());
        } catch (PaymentException e) {
            payment.setStatus(PaymentStatus.FAILED);
            failureReason = e.getMessage();
            log.error("Payment processing failed: paymentId={}, reason={}", 
                    payment.getKey().getPaymentId(), failureReason);
        }

        paymentRepository.save(payment);

        // 6. Publish Kafka event based on payment status
        PaymentEvent event = PaymentEvent.builder()
                .paymentId(payment.getKey().getPaymentId())
                .requestId(payment.getKey().getRequestId())
                .orderId(payment.getOrderId())
                .itemId(order.getItemId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .timestamp(LocalDateTime.now())
                .errorMessage(failureReason)  // Include error message for failed payments
                .build();

        // 7. If payment FAILED → restock items, mark order failed, publish event
        if (payment.getStatus() == PaymentStatus.FAILED) {
            try {
                log.info("Payment failed — restocking item {} quantity {}", order.getItemId(), order.getQuantity());
                itemServiceClient.restockItem(order.getItemId(), order.getQuantity());
                orderServiceClient.markOrderPaymentFailed(request.getOrderId());
                log.info("Stock restored and order {} marked as payment failed", request.getOrderId());
            } catch (Exception e) {
                log.error("Failed to restock after payment failure: {}", e.getMessage());
            }
            log.info("Publishing payment failed event to Kafka: paymentId={}, error={}", 
                    payment.getKey().getPaymentId(), failureReason);
            eventProducer.publishPaymentFailed(event);
        }

        // 8. If payment SUCCESS → confirm order, publish event
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            try {
                orderServiceClient.confirmOrder(request.getOrderId());
                log.info("Order {} confirmed — payment successful, stock deducted", request.getOrderId());
            } catch (Exception e) {
                log.error("Failed to confirm order: {}", e.getMessage());
            }
            eventProducer.publishPaymentSuccess(event);
        }

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

        // Restock items and update order status
        try {
            OrderDetailDto order = orderServiceClient.getOrderDetails(payment.getOrderId());
            log.info("Restocking item {} quantity {}", order.getItemId(), order.getQuantity());
            itemServiceClient.restockItem(order.getItemId(), order.getQuantity());
            orderServiceClient.refundOrder(payment.getOrderId());
            log.info("Order {} refunded and stock restored", payment.getOrderId());
            
            // Publish refund event to Kafka
            PaymentEvent event = PaymentEvent.builder()
                    .paymentId(payment.getKey().getPaymentId())
                    .requestId(payment.getKey().getRequestId())
                    .orderId(payment.getOrderId())
                    .itemId(order.getItemId())
                    .amount(payment.getAmount())
                    .currency(payment.getCurrency())
                    .status(PaymentStatus.REFUNDED)
                    .timestamp(LocalDateTime.now())
                    .build();
            eventProducer.publishPaymentRefunded(event);
        } catch (Exception e) {
            log.error("Failed to restock or refund order: {}", e.getMessage());
        }

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
}
