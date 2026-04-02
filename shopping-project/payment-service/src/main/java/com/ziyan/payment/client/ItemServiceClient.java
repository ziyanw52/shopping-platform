package com.ziyan.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * OpenFeign client for communicating with Item Service
 * Manages inventory operations
 */
@FeignClient(name = "item-service", url = "${item-service.url:http://localhost:8082}")
public interface ItemServiceClient {

    /**
     * Get item details (for price validation)
     * Endpoint: GET /items/{itemId}
     */
    @GetMapping("/items/{itemId}")
    ItemDetailDto getItem(@PathVariable String itemId);

    /**
     * Add items back to inventory on refund
     * Endpoint: POST /items/{itemId}/restock?quantity=X
     */
    @PostMapping("/items/{itemId}/restock")
    void restockItem(@PathVariable String itemId, @RequestParam Integer quantity);

    /**
     * Reduce inventory on successful payment
     * Endpoint: POST /items/{itemId}/reserve?quantity=X
     */
    @PostMapping("/items/{itemId}/reserve")
    void reserveItem(@PathVariable String itemId, @RequestParam Integer quantity);
}
