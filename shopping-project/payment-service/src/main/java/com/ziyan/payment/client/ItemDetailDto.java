package com.ziyan.payment.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Item details from Item Service
 * Used to validate payment amount against item price
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDetailDto {
    private String id;
    private String name;
    private Double price;
    private String upc;
    private Integer stock;
}
