package com.ziyan.order.dto;

public class UpdateOrderRequest {

    private Integer quantity;

    public UpdateOrderRequest() {
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}