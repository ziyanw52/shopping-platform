package com.ziyan.payment.event;

import com.ziyan.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment event published to Kafka
 * Used for async notification and idempotency tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String paymentId;
    private String requestId;  // For idempotency
    private Long orderId;
    private String itemId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime timestamp;
    private String eventType;  // PAYMENT_SUCCESS, PAYMENT_FAILED, PAYMENT_REFUNDED
    private String errorMessage;  // For failed payments - contains the failure reason
}
