package com.ziyan.order.controller;

import com.ziyan.order.dto.*;
import com.ziyan.order.service.OrderService;
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

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id){
        return orderService.getOrder(id);
    }

    @PutMapping("/{id}")
    public OrderResponse updateOrder(
            @PathVariable Long id,
            @RequestBody UpdateOrderRequest request){
        return orderService.updateOrder(id, request);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id){
        return orderService.cancelOrder(id);
    }

    @PostMapping("/{id}/paid")
    public OrderResponse markPaid(@PathVariable Long id){
        return orderService.markPaid(id);
    }

    @PostMapping("/{id}/complete")
    public OrderResponse completeOrder(@PathVariable Long id){
        return orderService.completeOrder(id);
    }
}