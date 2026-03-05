package com.ziyan.payment.kafka.listener;

import com.ziyan.payment.client.ItemServiceClient;
import com.ziyan.payment.client.OrderServiceClient;
import com.ziyan.payment.kafka.event.PaymentEvent;
import com.ziyan.payment.service.EmailService;
import com.ziyan.payment.service.RetryService;
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
    private final OrderServiceClient orderServiceClient;
    private final ItemServiceClient itemServiceClient;
    private final RetryService retryService;

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
            
            // Update order status to CONFIRMED
            orderServiceClient.confirmOrder(event.getOrderId());
            
            // TODO: Send success notification (push notification, SMS, etc)
            
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
            
            // Update order status to FAILED
            orderServiceClient.markOrderPaymentFailed(event.getOrderId());
            
            // Schedule retry task (will retry after 5 minutes, then exponential backoff)
            retryService.scheduleRetry(event.getPaymentId(), event.getFailureReason());
            
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
            
            // Update order status to REFUNDED
            orderServiceClient.refundOrder(event.getOrderId());
            
            // TODO: Add items back to inventory (need item details from order service)
            // itemServiceClient.restockItem(itemId, quantity);
            
            log.info("✓ Successfully processed payment refund event for Order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("✗ Error processing payment refund event: ", e);
        }
    }
}
