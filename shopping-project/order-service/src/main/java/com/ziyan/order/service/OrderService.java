package com.ziyan.order.service;

import com.ziyan.order.dto.CreateOrderRequest;
import com.ziyan.order.dto.UpdateOrderRequest;
import com.ziyan.order.dto.OrderResponse;
import com.ziyan.order.entity.Order;
import com.ziyan.order.enums.OrderStatus;
import com.ziyan.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Create Order
    public OrderResponse createOrder(CreateOrderRequest request) {

        Order order = new Order(
                request.getUserId(),
                request.getItemId(),
                request.getQuantity()
        );

        orderRepository.save(order);

        return convert(order);
    }

    // Get all orders
    public List<OrderResponse> getOrders() {
        return orderRepository.findAll()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    // Get order by id
    public OrderResponse getOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return convert(order);
    }

    // Update order
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setQuantity(request.getQuantity());

        orderRepository.save(order);

        return convert(order);
    }

    // Delete order
    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    // Convert entity -> response
    private OrderResponse convert(Order order) {

        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getItemId(),
                order.getQuantity()
        );
    }

    // Cancel order
    public OrderResponse cancelOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        return convert(order);
    }

    // Mark order paid
    public OrderResponse markPaid(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.PAID);

        orderRepository.save(order);

        return convert(order);
    }

    // Complete order
    public OrderResponse completeOrder(Long id) {

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.COMPLETED);

        orderRepository.save(order);

        return convert(order);
    }
}