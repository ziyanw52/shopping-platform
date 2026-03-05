package com.ziyan.payment.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client for communicating with Item Service
 * Manages inventory operations
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ItemServiceClient {

    @Value("${item-service.url:http://localhost:8083}")
    private String itemServiceUrl;

    private final RestTemplate restTemplate;

    /**
     * Add items back to inventory on refund
     * Expected endpoint: POST /items/{itemId}/restock?quantity=X
     */
    public void restockItem(Long itemId, Integer quantity) {
        try {
            String url = itemServiceUrl + "/items/" + itemId + "/restock?quantity=" + quantity;
            restTemplate.postForObject(url, null, Void.class);
            log.info("✓ Item {} restocked with quantity {}", itemId, quantity);
        } catch (Exception e) {
            log.error("✗ Failed to restock item {}: {}", itemId, e.getMessage());
        }
    }

    /**
     * Reduce inventory on successful payment
     * Expected endpoint: POST /items/{itemId}/reserve?quantity=X
     */
    public void reserveItem(Long itemId, Integer quantity) {
        try {
            String url = itemServiceUrl + "/items/" + itemId + "/reserve?quantity=" + quantity;
            restTemplate.postForObject(url, null, Void.class);
            log.info("✓ Item {} reserved with quantity {}", itemId, quantity);
        } catch (Exception e) {
            log.error("✗ Failed to reserve item {}: {}", itemId, e.getMessage());
        }
    }
}
