package com.ziyan.payment.service;

import java.math.BigDecimal;

/**
 * Email Service Interface
 * Handles sending payment-related emails to customers
 */
public interface EmailService {
    void sendPaymentSuccessEmail(Long orderId, BigDecimal amount);
    void sendPaymentFailureEmail(Long orderId, String failureReason);
    void sendPaymentRefundEmail(Long orderId, BigDecimal refundAmount);
}
