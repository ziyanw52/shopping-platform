package com.ziyan.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * OpenFeign client for communicating with Order Service
 * Updates order status based on payment events
 */
@FeignClient(name = "order-service", url = "${order-service.url:http://localhost:8083}")
public interface OrderServiceClient {

    /**
     * Update order status to CONFIRMED
     */
    @PostMapping("/orders/{orderId}/confirm")
    void confirmOrder(@PathVariable Long orderId);

    /**
     * Update order status to PAYMENT_FAILED
     */
    @PostMapping("/orders/{orderId}/payment-failed")
    void markOrderPaymentFailed(@PathVariable Long orderId);

    /**
     * Update order status to REFUNDED
     */
    @PostMapping("/orders/{orderId}/refund")
    void refundOrder(@PathVariable Long orderId);

    /**
     * Get order details (for fetching items to restock on refund)
     */
    @GetMapping("/orders/{orderId}")
    OrderDetailDto getOrderDetails(@PathVariable Long orderId);
}
