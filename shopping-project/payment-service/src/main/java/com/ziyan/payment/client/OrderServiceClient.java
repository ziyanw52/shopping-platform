package com.ziyan.payment.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for communicating with Order Service
 * Updates order status based on payment events
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderServiceClient {

    @Value("${order-service.url:http://localhost:8083}")
    private String orderServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * Update order status to CONFIRMED
     */
    public void confirmOrder(Long orderId) {
        try {
            String url = orderServiceUrl + "/orders/" + orderId + "/confirm";
            restTemplate.postForObject(url, null, Void.class);
            log.info("✓ Order {} status updated to CONFIRMED", orderId);
        } catch (Exception e) {
            log.error("✗ Failed to confirm order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Update order status to PAYMENT_FAILED
     */
    public void markOrderPaymentFailed(Long orderId) {
        try {
            String url = orderServiceUrl + "/orders/" + orderId + "/payment-failed";
            restTemplate.postForObject(url, null, Void.class);
            log.info("✓ Order {} status updated to PAYMENT_FAILED", orderId);
        } catch (Exception e) {
            log.error("✗ Failed to mark order {} as payment failed: {}", orderId, e.getMessage());
        }
    }

    /**
     * Update order status to REFUNDED
     */
    public void refundOrder(Long orderId) {
        try {
            String url = orderServiceUrl + "/orders/" + orderId + "/refund";
            restTemplate.postForObject(url, null, Void.class);
            log.info("✓ Order {} status updated to REFUNDED", orderId);
        } catch (Exception e) {
            log.error("✗ Failed to refund order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Get order details (for fetching items to restock on refund)
     */
    public OrderDetailDto getOrderDetails(Long orderId) {
        try {
            String url = orderServiceUrl + "/orders/" + orderId;
            OrderDetailDto order = restTemplate.getForObject(url, OrderDetailDto.class);
            log.info("✓ Retrieved order details for order: {}", orderId);
            return order;
        } catch (Exception e) {
            log.error("✗ Failed to get order details {}: {}", orderId, e.getMessage());
            return null;
        }
    }
}
