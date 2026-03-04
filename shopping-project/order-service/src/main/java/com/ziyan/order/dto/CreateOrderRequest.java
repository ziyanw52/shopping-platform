package com.ziyan.order.dto;

import lombok.Data;

@Data
public class CreateOrderRequest {

    private Long userId;
    private String itemId;
    private Integer quantity;
}