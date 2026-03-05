package com.ziyan.payment.service.impl;

import com.ziyan.payment.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Mock Email Service Implementation
 * Logs emails to console instead of sending them
 * Used for testing and development
 */
@Service
@Slf4j
public class MockEmailService implements EmailService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void sendPaymentSuccessEmail(Long orderId, BigDecimal amount) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        log.info("""
            
            ╔══════════════════════════════════════════════════════════╗
            ║                   📧 SUCCESS EMAIL                        ║
            ╚══════════════════════════════════════════════════════════╝
            To:       customer@example.com
            Subject:  ✓ Payment Confirmed - Order {}
            Date:     {}
            
            Dear Customer,
            
            Your payment has been successfully processed!
            
            Order ID:     {}
            Amount:       ${}
            Status:       CONFIRMED
            
            Your order is now being prepared and will ship soon.
            
            Thank you for your purchase!
            
            Best regards,
            Shopping Platform Team
            ╔══════════════════════════════════════════════════════════╗
            """, orderId, timestamp, orderId, amount);
    }

    @Override
    public void sendPaymentFailureEmail(Long orderId, String failureReason) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        log.warn("""
            
            ╔══════════════════════════════════════════════════════════╗
            ║                   📧 FAILURE EMAIL                        ║
            ╚══════════════════════════════════════════════════════════╝
            To:       customer@example.com
            Subject:  ✗ Payment Failed - Order {}
            Date:     {}
            
            Dear Customer,
            
            Unfortunately, your payment could not be processed.
            
            Order ID:     {}
            Reason:       {}
            
            Please try again or contact support for assistance.
            
            Retry Link:   https://shopping-platform.com/retry-payment/{}
            
            Thank you,
            Shopping Platform Team
            ╔══════════════════════════════════════════════════════════╗
            """, orderId, timestamp, orderId, failureReason, orderId);
    }

    @Override
    public void sendPaymentRefundEmail(Long orderId, BigDecimal refundAmount) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        log.info("""
            
            ╔══════════════════════════════════════════════════════════╗
            ║                   📧 REFUND EMAIL                         ║
            ╚══════════════════════════════════════════════════════════╝
            To:       customer@example.com
            Subject:  ↩️ Refund Processed - Order {}
            Date:     {}
            
            Dear Customer,
            
            Your refund has been processed successfully!
            
            Order ID:        {}
            Refund Amount:   ${}
            Status:          REFUNDED
            
            The funds will be credited to your original payment method
            within 3-5 business days.
            
            Thank you,
            Shopping Platform Team
            ╔══════════════════════════════════════════════════════════╗
            """, orderId, timestamp, orderId, refundAmount);
    }
}
