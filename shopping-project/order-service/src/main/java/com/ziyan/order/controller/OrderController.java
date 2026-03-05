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
}