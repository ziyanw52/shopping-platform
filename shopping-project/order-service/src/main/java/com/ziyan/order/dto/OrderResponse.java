package com.ziyan.order.dto;

import com.ziyan.order.enums.OrderStatus;

public class OrderResponse {

    private Long orderId;
    private Long userId;
    private String itemId;
    private Integer quantity;
    private OrderStatus status;

    public OrderResponse(Long orderId, Long userId, String itemId, Integer quantity, OrderStatus status) {
        this.orderId = orderId;
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
        this.status = status;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getItemId() {
        return itemId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }
}