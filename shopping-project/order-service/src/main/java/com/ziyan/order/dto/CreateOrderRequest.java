package com.ziyan.order.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {

    private Long userId;

    private String itemId;

    private Integer quantity;

    public CreateOrderRequest() {
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

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}