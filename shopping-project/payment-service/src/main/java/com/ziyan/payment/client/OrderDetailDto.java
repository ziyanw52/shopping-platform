package com.ziyan.payment.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Order details from Order Service
 * Used to get item information for restocking on refund
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailDto {
    private Long orderId;
    private Long userId;
    private String itemId;
    private Integer quantity;
    private String status;
}
