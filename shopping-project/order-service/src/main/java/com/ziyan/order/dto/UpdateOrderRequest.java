package com.ziyan.order.dto;

import jakarta.validation.constraints.Min;

public class UpdateOrderRequest {

    @Min(value = 0, message = "Quantity must be greater than or equal to 0")
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