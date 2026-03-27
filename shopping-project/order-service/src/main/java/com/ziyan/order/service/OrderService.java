package com.ziyan.order.service;

import com.ziyan.order.dto.*;
import com.ziyan.order.entity.Cart;
import com.ziyan.order.entity.CartItem;
import com.ziyan.order.entity.Order;
import com.ziyan.order.enums.OrderStatus;
import com.ziyan.order.repository.OrderRepository;
import com.ziyan.order.repository.CartRepository;
import com.ziyan.order.repository.CartItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public OrderService(OrderRepository orderRepository, CartRepository cartRepository, CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
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
    public OrderResponse getOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        return convert(order);
    }

    // Update order
    public OrderResponse updateOrder(Long orderId, UpdateOrderRequest request) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setQuantity(request.getQuantity());

        orderRepository.save(order);

        return convert(order);
    }

    // Delete order
    public void deleteOrder(Long orderId) {
        orderRepository.deleteById(orderId);
    }

    // Convert entity -> response
    private OrderResponse convert(Order order) {

        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getItemId(),
                order.getQuantity(),
                order.getStatus()
        );
    }

    // Cancel order
    public OrderResponse cancelOrder(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CANCELLED);

        orderRepository.save(order);

        return convert(order);
    }

    // Mark order paid
    public OrderResponse markPaid(Long orderId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CONFIRMED);

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

    // Confirm order (payment successful)
    public OrderResponse confirmOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        return convert(order);
    }

    // Mark order payment failed
    public OrderResponse markPaymentFailed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        orderRepository.save(order);

        return convert(order);
    }

    // Refund order
    public OrderResponse refundOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        return convert(order);
    }

    // ============= CART METHODS =============

    // Get or create cart for user
    public CartResponse getOrCreateCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart(userId);
                    return cartRepository.save(newCart);
                });
        return convertCart(cart);
    }

    // Get cart by user ID
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
        return convertCart(cart);
    }

    // Add item to cart
    public CartResponse addItemToCart(Long userId, AddToCartRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = new Cart(userId);
                    return cartRepository.save(newCart);
                });

        // Check if item already exists in cart
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getItemId().equals(request.getItemId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Update quantity if item already in cart
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
        } else {
            // Add new item to cart
            CartItem newItem = new CartItem(
                    request.getItemId(),
                    request.getItemName(),
                    request.getItemPrice(),
                    request.getQuantity()
            );
            cart.addItem(newItem);
        }

        cartRepository.save(cart);
        return convertCart(cart);
    }

    // Remove item from cart
    public CartResponse removeItemFromCart(Long userId, Long cartItemId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem itemToRemove = cart.getItems().stream()
                .filter(item -> item.getCartItemId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        cart.removeItem(itemToRemove);
        cartRepository.save(cart);

        return convertCart(cart);
    }

    // Update item quantity in cart
    public CartResponse updateItemQuantity(Long userId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getCartItemId().equals(request.getCartItemId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Item not found in cart"));

        if (request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity must be greater than 0");
        }

        item.setQuantity(request.getQuantity());
        cartRepository.save(cart);

        return convertCart(cart);
    }

    // Clear cart
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        cart.clearCart();
        cartRepository.save(cart);
    }

    // Get cart total
    public Double getCartTotal(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        return cart.getTotalPrice();
    }

    // Get cart item count
    public Integer getCartItemCount(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        return cart.getTotalQuantity();
    }

    // Checkout - convert cart to order
    public OrderResponse checkout(Long userId) {
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cannot checkout with empty cart");
        }

        // Create order from first cart item (simplified for single item orders)
        // In a real system, you'd create an order with multiple items
        CartItem firstItem = cart.getItems().get(0);

        Order order = new Order(
                userId,
                firstItem.getItemId(),
                firstItem.getQuantity()
        );

        orderRepository.save(order);

        // Clear cart after checkout
        cart.clearCart();
        cartRepository.save(cart);

        return convert(order);
    }

    // Convert Cart entity to CartResponse DTO
    private CartResponse convertCart(Cart cart) {
        List<CartResponse.CartItemDTO> itemDTOs = cart.getItems().stream()
                .map(item -> new CartResponse.CartItemDTO(
                        item.getCartItemId(),
                        item.getItemId(),
                        item.getItemName(),
                        item.getItemPrice(),
                        item.getQuantity(),
                        item.getSubtotal()
                ))
                .collect(Collectors.toList());

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .userId(cart.getUserId())
                .items(itemDTOs)
                .totalPrice(cart.getTotalPrice())
                .totalQuantity(cart.getTotalQuantity())
                .build();
    }
}