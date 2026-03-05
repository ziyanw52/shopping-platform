package com.ziyan.payment.kafka.listener;

import com.ziyan.payment.kafka.event.PaymentEvent;
import com.ziyan.payment.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Kafka Consumer for Payment Events
 * Listens to 3 topics: payment.success, payment.failed, payment.refunded
 * Triggers notifications and updates to other services
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentEventListener {

    private final EmailService emailService;

    /**
     * Handle successful payments
     * Topics: payment.success
     */
    @KafkaListener(
        topics = "payment.success",
        groupId = "payment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentSuccess(PaymentEvent event) {
        log.info("✓ PAYMENT SUCCESS EVENT: PaymentId={}, OrderId={}, Amount={} {}",
            event.getPaymentId(),
            event.getOrderId(),
            event.getAmount(),
            event.getCurrency());
        
        try {
            // Send success email to customer
            emailService.sendPaymentSuccessEmail(event.getOrderId(), event.getAmount());
            
            // TODO: Update order status to CONFIRMED
            // TODO: Notify order service
            
            log.info("✓ Successfully processed payment success event for Order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("✗ Error processing payment success event: ", e);
        }
    }

    /**
     * Handle failed payments
     * Topics: payment.failed
     */
    @KafkaListener(
        topics = "payment.failed",
        groupId = "payment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentFailed(PaymentEvent event) {
        log.warn("✗ PAYMENT FAILED EVENT: PaymentId={}, OrderId={}, Amount={} {}",
            event.getPaymentId(),
            event.getOrderId(),
            event.getAmount(),
            event.getCurrency());
        
        try {
            // Send failure email with retry link
            emailService.sendPaymentFailureEmail(event.getOrderId(), event.getFailureReason());
            
            // TODO: Update order status to FAILED
            // TODO: Create retry task (retry after 5 minutes)
            
            log.info("✓ Successfully processed payment failed event for Order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("✗ Error processing payment failed event: ", e);
        }
    }

    /**
     * Handle refunded payments
     * Topics: payment.refunded
     */
    @KafkaListener(
        topics = "payment.refunded",
        groupId = "payment-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPaymentRefunded(PaymentEvent event) {
        log.info("↩️  PAYMENT REFUNDED EVENT: PaymentId={}, OrderId={}, Amount={} {}",
            event.getPaymentId(),
            event.getOrderId(),
            event.getAmount(),
            event.getCurrency());
        
        try {
            // Send refund confirmation email
            emailService.sendPaymentRefundEmail(event.getOrderId(), event.getAmount());
            
            // TODO: Update order status to REFUNDED
            // TODO: Add items back to inventory
            
            log.info("✓ Successfully processed payment refund event for Order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("✗ Error processing payment refund event: ", e);
        }
    }
}
