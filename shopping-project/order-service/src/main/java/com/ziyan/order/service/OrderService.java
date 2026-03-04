package com.ziyan.order.service;

import com.ziyan.order.dto.CreateOrderRequest;
import com.ziyan.order.entity.Order;
import com.ziyan.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public Order createOrder(CreateOrderRequest request) {

        // call item-service to deduce inventory
        String itemServiceUrl = "http://localhost:8082/items/"
                + request.getItemId()
                + "/deduct?quantity="
                + request.getQuantity();

        try {
            restTemplate.postForObject(itemServiceUrl, null, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deduct inventory");
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setItemId(request.getItemId());
        order.setQuantity(request.getQuantity());
        order.setStatus("CREATED");

        return orderRepository.save(order);
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
}