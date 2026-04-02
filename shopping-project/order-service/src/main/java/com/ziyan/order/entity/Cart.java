package com.ziyan.order.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @Column(unique = true)
    private Long userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<CartItem> items = new ArrayList<>();

    private Double totalPrice;
    private int totalQuantity;

    public Cart() {}

    public Cart(Long userId) {
        this.userId = userId;
        this.totalPrice = 0.0;
        this.totalQuantity = 0;
    }

    // Getters and Setters
    public Long getCartId() {
        return cartId;
    }

    public void setCartId(Long cartId) {
        this.cartId = cartId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public int getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(int totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public void addItem(CartItem item) {
        if (this.items == null) {
            this.items = new java.util.ArrayList<>();
        }
        item.setCart(this);
        this.items.add(item);
        this.totalQuantity += item.getQuantity();
        this.totalPrice = this.totalPrice + item.getSubtotal();
    }

    public void removeItem(CartItem item) {
        if (this.items != null) {
            this.items.remove(item);
            this.totalQuantity -= item.getQuantity();
            this.totalPrice = this.totalPrice - item.getSubtotal();
        }
    }

    public void clearCart() {
        this.items.clear();
        this.totalQuantity = 0;
        this.totalPrice = 0.0;
    }
}
