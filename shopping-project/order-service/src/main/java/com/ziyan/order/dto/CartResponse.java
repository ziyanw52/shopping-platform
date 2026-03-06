package com.ziyan.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private Long cartId;
    private Long userId;
    private List<CartItemDTO> items;
    private Double totalPrice;
    private Integer totalQuantity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemDTO {
        private Long cartItemId;
        private String itemId;
        private String itemName;
        private Double itemPrice;
        private Integer quantity;
        private Double subtotal;
    }
}
