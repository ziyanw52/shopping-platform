package com.ziyan.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    private String itemId;
    private String itemName;
    private Double itemPrice;
    private Integer quantity;
}
