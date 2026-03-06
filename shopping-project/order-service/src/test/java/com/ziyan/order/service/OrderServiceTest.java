package com.ziyan.order.service;

import com.ziyan.order.dto.*;
import com.ziyan.order.entity.Cart;
import com.ziyan.order.entity.CartItem;
import com.ziyan.order.entity.Order;
import com.ziyan.order.enums.OrderStatus;
import com.ziyan.order.repository.OrderRepository;
import com.ziyan.order.repository.CartRepository;
import com.ziyan.order.repository.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService
 * Tests order creation, retrieval, updates, deletion, and cart functionality
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest createOrderRequest;
    private UpdateOrderRequest updateOrderRequest;
    private Order testOrder;
    private Order testOrder2;

    @BeforeEach
    void setUp() {
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(1L);

        updateOrderRequest = new UpdateOrderRequest();
        updateOrderRequest.setQuantity(3);

        testOrder = new Order(1L, "ITEM1", 2);
        testOrder2 = new Order(2L, "ITEM2", 1);
    }

    // ============ ORDER TESTS ============

    @Test
    void testCreateOrderSuccess() {
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        OrderResponse result = orderService.createOrder(createOrderRequest);
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testGetOrders() {
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder, testOrder2));
        List<OrderResponse> result = orderService.getOrders();
        assertEquals(2, result.size());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testGetOrderById() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        OrderResponse result = orderService.getOrder(1L);
        assertNotNull(result);
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> orderService.getOrder(999L));
    }

    @Test
    void testUpdateOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        OrderResponse result = orderService.updateOrder(1L, updateOrderRequest);
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testDeleteOrder() {
        orderService.deleteOrder(1L);
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void testCancelOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        OrderResponse result = orderService.cancelOrder(1L);
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
    }

    @Test
    void testMarkPaid() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        OrderResponse result = orderService.markPaid(1L);
        assertNotNull(result);
    }

    @Test
    void testConfirmOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        OrderResponse result = orderService.confirmOrder(1L);
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, testOrder.getStatus());
    }

    @Test
    void testCompleteOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        OrderResponse result = orderService.completeOrder(1L);
        assertNotNull(result);
        assertEquals(OrderStatus.COMPLETED, testOrder.getStatus());
    }

    @Test
    void testRefundOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        OrderResponse result = orderService.refundOrder(1L);
        assertNotNull(result);
        assertEquals(OrderStatus.REFUNDED, testOrder.getStatus());
    }

    // ============ CART TESTS ============

    @Test
    void testGetOrCreateCart_NewCart() {
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        Cart newCart = new Cart(userId);
        newCart.setCartId(1L);
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        CartResponse response = orderService.getOrCreateCart(userId);
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testGetOrCreateCart_ExistingCart() {
        Long userId = 1L;
        Cart existingCart = new Cart(userId);
        existingCart.setCartId(1L);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(existingCart));
        CartResponse response = orderService.getOrCreateCart(userId);
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(cartRepository, times(0)).save(any(Cart.class));
    }

    @Test
    void testGetCart_Success() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        CartResponse response = orderService.getCart(userId);
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void testGetCart_NotFound() {
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> orderService.getCart(userId));
    }

    @Test
    void testAddItemToCart_NewItem() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        AddToCartRequest request = new AddToCartRequest("ITEM1", "Test Item", 99.99, 2);
        CartResponse response = orderService.addItemToCart(userId, request);
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
    }

    @Test
    void testAddItemToCart_ExistingItem() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem existingItem = new CartItem("ITEM1", "Test Item", 99.99, 2);
        existingItem.setCartItemId(1L);
        cart.setItems(Arrays.asList(existingItem));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        AddToCartRequest request = new AddToCartRequest("ITEM1", "Test Item", 99.99, 3);
        CartResponse response = orderService.addItemToCart(userId, request);
        assertNotNull(response);
        assertEquals(5, existingItem.getQuantity());
    }

    @Test
    void testRemoveItemFromCart_Success() {
        Long userId = 1L;
        Long cartItemId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(cartItemId);
        cart.setItems(new ArrayList<>(Arrays.asList(item)));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        CartResponse response = orderService.removeItemFromCart(userId, cartItemId);
        assertNotNull(response);
    }

    @Test
    void testRemoveItemFromCart_ItemNotFound() {
        Long userId = 1L;
        Long cartItemId = 999L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        assertThrows(RuntimeException.class, () -> orderService.removeItemFromCart(userId, cartItemId));
    }

    @Test
    void testUpdateItemQuantity_Success() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(1L);
        cart.setItems(new ArrayList<>(Arrays.asList(item)));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        UpdateCartItemRequest request = new UpdateCartItemRequest(1L, 5);
        CartResponse response = orderService.updateItemQuantity(userId, request);
        assertNotNull(response);
        assertEquals(5, item.getQuantity());
    }

    @Test
    void testUpdateItemQuantity_InvalidQuantity() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(1L);
        cart.setItems(Arrays.asList(item));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        UpdateCartItemRequest request = new UpdateCartItemRequest(1L, 0);
        assertThrows(RuntimeException.class, () -> orderService.updateItemQuantity(userId, request));
    }

    @Test
    void testClearCart() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        cart.setItems(new ArrayList<>(Arrays.asList(item)));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        orderService.clearCart(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testGetCartTotal() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem item1 = new CartItem("ITEM1", "Test Item 1", 100.00, 2);
        CartItem item2 = new CartItem("ITEM2", "Test Item 2", 50.00, 1);
        cart.setItems(Arrays.asList(item1, item2));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        Double total = orderService.getCartTotal(userId);
        assertEquals(250.00, total);
    }

    @Test
    void testGetCartItemCount() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem item1 = new CartItem("ITEM1", "Test Item 1", 100.00, 2);
        CartItem item2 = new CartItem("ITEM2", "Test Item 2", 50.00, 3);
        cart.setItems(Arrays.asList(item1, item2));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        Integer count = orderService.getCartItemCount(userId);
        assertEquals(5, count);
    }

    @Test
    void testCheckout_Success() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(1L);
        cart.setItems(new ArrayList<>(Arrays.asList(item)));
        Order createdOrder = new Order(userId, "ITEM1", 2);
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(createdOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);
        OrderResponse response = orderService.checkout(userId);
        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCheckout_EmptyCart() {
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(new ArrayList<>());
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        assertThrows(RuntimeException.class, () -> orderService.checkout(userId));
    }

    @Test
    void testCheckout_CartNotFound() {
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> orderService.checkout(userId));
    }
}
