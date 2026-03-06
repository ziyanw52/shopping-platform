package com.ziyan.order.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@NoArgsConstructor
@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartItemId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private String itemId;

    @Column(nullable = false)
    private String itemName;

    @Column(nullable = false)
    private Double itemPrice;

    @Column(nullable = false)
    private Integer quantity;

    public CartItem(String itemId, String itemName, Double itemPrice, Integer quantity) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
    }

    public Double getSubtotal() {
        return itemPrice * quantity;
    }
}
