package com.ziyan.order.controller;

import com.ziyan.order.dto.*;
import com.ziyan.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request){
        return orderService.createOrder(request);
    }

    @GetMapping
    public List<OrderResponse> getOrders(){
        return orderService.getOrders();
    }

    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable Long orderId){
        return orderService.getOrder(orderId);
    }

    @PutMapping("/{orderId}")
    public OrderResponse updateOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderRequest request){
        return orderService.updateOrder(orderId, request);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long orderId){
        return orderService.cancelOrder(orderId);
    }

    @PostMapping("/{orderId}/paid")
    public OrderResponse markPaid(@PathVariable Long orderId){
        return orderService.markPaid(orderId);
    }

    @PostMapping("/{id}/complete")
    public OrderResponse completeOrder(@PathVariable Long id){
        return orderService.completeOrder(id);
    }

    @PostMapping("/{orderId}/confirm")
    public OrderResponse confirmOrder(@PathVariable Long orderId){
        return orderService.confirmOrder(orderId);
    }

    @PostMapping("/{orderId}/payment-failed")
    public OrderResponse markPaymentFailed(@PathVariable Long orderId){
        return orderService.markPaymentFailed(orderId);
    }

    @PostMapping("/{orderId}/refund")
    public OrderResponse refundOrder(@PathVariable Long orderId){
        return orderService.refundOrder(orderId);
    }

    // ============= CART ENDPOINTS =============

    @GetMapping("/carts/{userId}")
    public CartResponse getCart(@PathVariable Long userId) {
        return orderService.getOrCreateCart(userId);
    }

    @PostMapping("/carts/{userId}/items")
    public CartResponse addItemToCart(
            @PathVariable Long userId,
            @RequestBody AddToCartRequest request) {
        return orderService.addItemToCart(userId, request);
    }

    @DeleteMapping("/carts/{userId}/items/{cartItemId}")
    public CartResponse removeItemFromCart(
            @PathVariable Long userId,
            @PathVariable Long cartItemId) {
        return orderService.removeItemFromCart(userId, cartItemId);
    }

    @PutMapping("/carts/{userId}/items")
    public CartResponse updateItemQuantity(
            @PathVariable Long userId,
            @RequestBody UpdateCartItemRequest request) {
        return orderService.updateItemQuantity(userId, request);
    }

    @GetMapping("/carts/{userId}/total")
    public Double getCartTotal(@PathVariable Long userId) {
        return orderService.getCartTotal(userId);
    }

    @GetMapping("/carts/{userId}/count")
    public Integer getCartItemCount(@PathVariable Long userId) {
        return orderService.getCartItemCount(userId);
    }

    @DeleteMapping("/carts/{userId}/clear")
    public void clearCart(@PathVariable Long userId) {
        orderService.clearCart(userId);
    }

    @PostMapping("/carts/{userId}/checkout")
    public OrderResponse checkout(@PathVariable Long userId) {
        return orderService.checkout(userId);
    }
}