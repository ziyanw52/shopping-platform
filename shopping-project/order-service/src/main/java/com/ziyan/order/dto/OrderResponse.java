package com.ziyan.order.dto;

public class OrderResponse {

    private Long id;
    private Long userId;
    private String itemId;
    private Integer quantity;

    public OrderResponse(Long id, Long userId, String itemId, Integer quantity) {
        this.id = id;
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
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
}