package com.ziyan.order.service;

import com.ziyan.order.dto.*;
import com.ziyan.order.entity.Cart;
import com.ziyan.order.entity.CartItem;
import com.ziyan.order.entity.Order;
import com.ziyan.order.repository.OrderRepository;
import com.ziyan.order.repository.CartRepository;
import com.ziyan.order.repository.CartItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService
 * Tests order creation, retrieval, updates, and deletion functionality
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
        // Setup create order request
        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setUserId(1L);
        createOrderRequest.setTotalPrice(299.97);
        createOrderRequest.setStatus("PENDING");

        // Setup update order request
        updateOrderRequest = new UpdateOrderRequest();
        updateOrderRequest.setQuantity(3);
        updateOrderRequest.setStatus("PROCESSING");

        // Setup test order
        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUserId(1L);
        testOrder.setTotalPrice(299.97);
        testOrder.setStatus("PENDING");

        // Setup second test order
        testOrder2 = new Order();
        testOrder2.setId(2L);
        testOrder2.setUserId(2L);
        testOrder2.setTotalPrice(199.98);
        testOrder2.setStatus("COMPLETED");
    }

    // ============ Order Creation Tests ============

    @Test
    void testCreateOrderSuccess() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        Order result = orderService.createOrder(createOrderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(299.97, result.getTotalPrice());
        assertEquals("PENDING", result.getStatus());
        assertNotNull(result.getCreatedAt());

        // Verify
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testCreateOrderWithZeroTotalPrice() {
        // Arrange
        CreateOrderRequest zeroRequest = new CreateOrderRequest();
        zeroRequest.setUserId(1L);
        zeroRequest.setTotalPrice(0.0);
        zeroRequest.setStatus("FREE");

        Order zeroOrder = new Order();
        zeroOrder.setId(1L);
        zeroOrder.setUserId(1L);
        zeroOrder.setTotalPrice(0.0);
        zeroOrder.setStatus("FREE");

        when(orderRepository.save(any(Order.class))).thenReturn(zeroOrder);

        // Act
        Order result = orderService.createOrder(zeroRequest);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getTotalPrice());
    }

    @Test
    void testCreateOrderWithNegativePrice() {
        // Arrange
        CreateOrderRequest negativeRequest = new CreateOrderRequest();
        negativeRequest.setUserId(1L);
        negativeRequest.setTotalPrice(-100.0);
        negativeRequest.setStatus("INVALID");

        Order negativeOrder = new Order();
        negativeOrder.setId(1L);
        negativeOrder.setTotalPrice(-100.0);

        when(orderRepository.save(any(Order.class))).thenReturn(negativeOrder);

        // Act & Assert
        assertDoesNotThrow(() -> orderService.createOrder(negativeRequest));
    }

    @Test
    void testCreateOrderWithNullStatus() {
        // Arrange
        CreateOrderRequest nullStatusRequest = new CreateOrderRequest();
        nullStatusRequest.setUserId(1L);
        nullStatusRequest.setTotalPrice(100.0);
        nullStatusRequest.setStatus(null);

        Order orderWithNullStatus = new Order();
        orderWithNullStatus.setId(1L);
        orderWithNullStatus.setStatus(null);

        when(orderRepository.save(any(Order.class))).thenReturn(orderWithNullStatus);

        // Act
        Order result = orderService.createOrder(nullStatusRequest);

        // Assert
        assertNull(result.getStatus());
    }

    @Test
    void testCreateOrderWithLargePrice() {
        // Arrange
        CreateOrderRequest largeRequest = new CreateOrderRequest();
        largeRequest.setUserId(1L);
        largeRequest.setTotalPrice(999999.99);
        largeRequest.setStatus("PENDING");

        Order largeOrder = new Order();
        largeOrder.setId(1L);
        largeOrder.setTotalPrice(999999.99);

        when(orderRepository.save(any(Order.class))).thenReturn(largeOrder);

        // Act
        Order result = orderService.createOrder(largeRequest);

        // Assert
        assertEquals(999999.99, result.getTotalPrice());
    }

    // ============ Order Retrieval Tests ============

    @Test
    void testGetOrdersSuccess() {
        // Arrange
        List<Order> orders = Arrays.asList(testOrder, testOrder2);
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<Order> result = orderService.getOrders();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        // Verify
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testGetOrdersEmptyList() {
        // Arrange
        when(orderRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Order> result = orderService.getOrders();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testGetOrdersSingleOrder() {
        // Arrange
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder));

        // Act
        List<Order> result = orderService.getOrders();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void testGetOrderByIdSuccess() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        Order result = orderService.getOrder(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PENDING", result.getStatus());

        // Verify
        verify(orderRepository, times(1)).findById(1L);
    }

    @Test
    void testGetOrderByIdNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Order result = orderService.getOrder(999L);

        // Assert
        assertNull(result);
        verify(orderRepository, times(1)).findById(999L);
    }

    @Test
    void testGetOrderByIdWithZeroId() {
        // Arrange
        when(orderRepository.findById(0L)).thenReturn(Optional.empty());

        // Act
        Order result = orderService.getOrder(0L);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetOrderByIdWithNegativeId() {
        // Arrange
        when(orderRepository.findById(-1L)).thenReturn(Optional.empty());

        // Act
        Order result = orderService.getOrder(-1L);

        // Assert
        assertNull(result);
    }

    // ============ Order Update Tests ============

    @Test
    void testUpdateOrderSuccess() {
        // Arrange
        Order orderToUpdate = new Order();
        orderToUpdate.setId(1L);
        orderToUpdate.setUserId(1L);
        orderToUpdate.setTotalPrice(299.97);
        orderToUpdate.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(orderToUpdate));
        when(orderRepository.save(any(Order.class))).thenReturn(orderToUpdate);

        // Act
        Order result = orderService.updateOrder(1L, updateOrderRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("PROCESSING", orderToUpdate.getStatus());

        // Verify
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testUpdateOrderNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> orderService.updateOrder(999L, updateOrderRequest)
        );

        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void testUpdateOrderWithNewStatus() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        UpdateOrderRequest statusUpdate = new UpdateOrderRequest();
        statusUpdate.setStatus("SHIPPED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = orderService.updateOrder(1L, statusUpdate);

        // Assert
        assertNotNull(result);
        assertEquals("SHIPPED", order.getStatus());
    }

    @Test
    void testUpdateOrderWithQuantity() {
        // Arrange
        Order order = new Order();
        order.setId(1L);

        UpdateOrderRequest quantityUpdate = new UpdateOrderRequest();
        quantityUpdate.setQuantity(5);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act
        Order result = orderService.updateOrder(1L, quantityUpdate);

        // Assert
        assertNotNull(result);
    }

    @Test
    void testUpdateOrderMultipleTimes() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        // Act - First update
        UpdateOrderRequest update1 = new UpdateOrderRequest();
        update1.setStatus("PROCESSING");
        orderService.updateOrder(1L, update1);

        // Act - Second update
        UpdateOrderRequest update2 = new UpdateOrderRequest();
        update2.setStatus("SHIPPED");
        orderService.updateOrder(1L, update2);

        // Assert
        assertEquals("SHIPPED", order.getStatus());
        verify(orderRepository, times(2)).findById(1L);
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    // ============ Order Deletion Tests ============

    @Test
    void testDeleteOrderSuccess() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act
        orderService.deleteOrder(1L);

        // Assert & Verify
        verify(orderRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteOrderNotFound() {
        // Arrange
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> orderService.deleteOrder(999L)
        );

        verify(orderRepository, times(1)).findById(999L);
        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteOrderWithZeroId() {
        // Arrange
        when(orderRepository.findById(0L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> orderService.deleteOrder(0L)
        );

        verify(orderRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteMultipleOrders() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.findById(2L)).thenReturn(Optional.of(testOrder2));

        // Act
        orderService.deleteOrder(1L);
        orderService.deleteOrder(2L);

        // Assert & Verify
        verify(orderRepository, times(1)).deleteById(1L);
        verify(orderRepository, times(1)).deleteById(2L);
    }

    // ============ Multiple Operations Tests ============

    @Test
    void testCreateAndRetrieveOrder() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act - Create
        Order created = orderService.createOrder(createOrderRequest);
        assertNotNull(created);

        // Act - Retrieve
        Order retrieved = orderService.getOrder(1L);

        // Assert
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void testCreateUpdateAndDeleteOrder() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // Act - Create
        Order created = orderService.createOrder(createOrderRequest);
        assertEquals("PENDING", created.getStatus());

        // Act - Update
        UpdateOrderRequest update = new UpdateOrderRequest();
        update.setStatus("SHIPPED");
        Order updated = orderService.updateOrder(1L, update);
        assertEquals("SHIPPED", updated.getStatus());

        // Act - Delete
        orderService.deleteOrder(1L);

        // Verify
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderRepository, times(2)).findById(1L);
        verify(orderRepository, times(1)).deleteById(1L);
    }

    @Test
    void testMultipleOrdersLifecycle() {
        // Arrange
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder, testOrder2);
        when(orderRepository.findAll()).thenReturn(Arrays.asList(testOrder, testOrder2));

        // Act - Create multiple orders
        Order order1 = orderService.createOrder(createOrderRequest);
        Order order2 = orderService.createOrder(createOrderRequest);

        // Act - Retrieve all
        List<Order> allOrders = orderService.getOrders();

        // Assert
        assertEquals(2, allOrders.size());
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testOrderStatusTransitions() {
        // Arrange
        Order order = new Order();
        order.setId(1L);
        order.setStatus("PENDING");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        String[] statuses = {"PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "COMPLETED"};

        // Act & Assert
        for (String status : statuses) {
            UpdateOrderRequest request = new UpdateOrderRequest();
            request.setStatus(status);
            Order updated = orderService.updateOrder(1L, request);
            assertEquals(status, updated.getStatus());
        }

        verify(orderRepository, times(5)).save(any(Order.class));
    }

    // ============= CART TESTS =============

    @Test
    void testGetOrCreateCart_NewCart() {
        // Arrange
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        
        Cart newCart = new Cart(userId);
        newCart.setCartId(1L);
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);

        // Act
        CartResponse response = orderService.getOrCreateCart(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(0, response.getTotalQuantity());
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testGetOrCreateCart_ExistingCart() {
        // Arrange
        Long userId = 1L;
        Cart existingCart = new Cart(userId);
        existingCart.setCartId(1L);
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(existingCart));

        // Act
        CartResponse response = orderService.getOrCreateCart(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(0)).save(any(Cart.class));
    }

    @Test
    void testGetCart_Success() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act
        CartResponse response = orderService.getCart(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals(1L, response.getCartId());
    }

    @Test
    void testGetCart_NotFound() {
        // Arrange
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.getCart(userId));
    }

    @Test
    void testAddItemToCart_NewItem() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(Collections.emptyList());
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        AddToCartRequest request = new AddToCartRequest("ITEM1", "Test Item", 99.99, 2);

        // Act
        CartResponse response = orderService.addItemToCart(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        verify(cartRepository, times(1)).findByUserId(userId);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testAddItemToCart_ExistingItem() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem existingItem = new CartItem("ITEM1", "Test Item", 99.99, 2);
        existingItem.setCartItemId(1L);
        cart.setItems(Arrays.asList(existingItem));
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        AddToCartRequest request = new AddToCartRequest("ITEM1", "Test Item", 99.99, 3);

        // Act
        CartResponse response = orderService.addItemToCart(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(5, existingItem.getQuantity()); // 2 + 3
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testRemoveItemFromCart_Success() {
        // Arrange
        Long userId = 1L;
        Long cartItemId = 1L;
        
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(cartItemId);
        cart.setItems(Arrays.asList(item));
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        CartResponse response = orderService.removeItemFromCart(userId, cartItemId);

        // Assert
        assertNotNull(response);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testRemoveItemFromCart_ItemNotFound() {
        // Arrange
        Long userId = 1L;
        Long cartItemId = 999L;
        
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(Collections.emptyList());
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.removeItemFromCart(userId, cartItemId));
    }

    @Test
    void testUpdateItemQuantity_Success() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(1L);
        cart.setItems(Arrays.asList(item));
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        UpdateCartItemRequest request = new UpdateCartItemRequest(1L, 5);

        // Act
        CartResponse response = orderService.updateItemQuantity(userId, request);

        // Assert
        assertNotNull(response);
        assertEquals(5, item.getQuantity());
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testUpdateItemQuantity_InvalidQuantity() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(1L);
        cart.setItems(Arrays.asList(item));
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        UpdateCartItemRequest request = new UpdateCartItemRequest(1L, 0);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.updateItemQuantity(userId, request));
    }

    @Test
    void testUpdateItemQuantity_ItemNotFound() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(Collections.emptyList());
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        UpdateCartItemRequest request = new UpdateCartItemRequest(999L, 5);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.updateItemQuantity(userId, request));
    }

    @Test
    void testClearCart() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        cart.setItems(Arrays.asList(item));
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        orderService.clearCart(userId);

        // Assert
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testGetCartTotal() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem item1 = new CartItem("ITEM1", "Test Item 1", 100.00, 2);
        CartItem item2 = new CartItem("ITEM2", "Test Item 2", 50.00, 1);
        cart.setItems(Arrays.asList(item1, item2));
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act
        Double total = orderService.getCartTotal(userId);

        // Assert
        assertEquals(250.00, total); // (100*2) + (50*1)
    }

    @Test
    void testGetCartItemCount() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem item1 = new CartItem("ITEM1", "Test Item 1", 100.00, 2);
        CartItem item2 = new CartItem("ITEM2", "Test Item 2", 50.00, 3);
        cart.setItems(Arrays.asList(item1, item2));
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act
        Integer count = orderService.getCartItemCount(userId);

        // Assert
        assertEquals(5, count); // 2 + 3
    }

    @Test
    void testCheckout_Success() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        
        CartItem item = new CartItem("ITEM1", "Test Item", 99.99, 2);
        item.setCartItemId(1L);
        cart.setItems(Arrays.asList(item));
        
        Order createdOrder = new Order();
        createdOrder.setId(100L);
        createdOrder.setUserId(userId);
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(orderRepository.save(any(Order.class))).thenReturn(createdOrder);
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        // Act
        OrderResponse response = orderService.checkout(userId);

        // Assert
        assertNotNull(response);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    void testCheckout_EmptyCart() {
        // Arrange
        Long userId = 1L;
        Cart cart = new Cart(userId);
        cart.setCartId(1L);
        cart.setItems(Collections.emptyList());
        
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.checkout(userId));
    }

    @Test
    void testCheckout_CartNotFound() {
        // Arrange
        Long userId = 1L;
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> orderService.checkout(userId));
    }
}
