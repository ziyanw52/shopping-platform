package com.ziyan.payment.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment event published to Kafka
 * Topics: payment.success, payment.failed, payment.refunded
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private String currency;
}
