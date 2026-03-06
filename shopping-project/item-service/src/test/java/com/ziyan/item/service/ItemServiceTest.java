package com.ziyan.item.service;

import com.ziyan.item.entity.Item;
import com.ziyan.item.repository.ItemRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ItemService
 * Tests item creation, retrieval, stock management, and inventory operations
 */
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private Item testItem2;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId("item-1");
        testItem.setName("Laptop");
        testItem.setPrice(999.99);
        testItem.setStock(50);
        testItem.setUpc("123456789");

        testItem2 = new Item();
        testItem2.setId("item-2");
        testItem2.setName("Mouse");
        testItem2.setPrice(29.99);
        testItem2.setStock(100);
        testItem2.setUpc("987654321");
    }

    // ============ Item Creation Tests ============

    @Test
    void testCreateItemSuccess() {
        // Arrange
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);

        // Act
        Item result = itemService.create(testItem);

        // Assert
        assertNotNull(result);
        assertEquals("item-1", result.getId());
        assertEquals("Laptop", result.getName());
        assertEquals(999.99, result.getPrice());
        assertEquals(50, result.getStock());

        // Verify
        verify(itemRepository, times(1)).save(testItem);
    }

    @Test
    void testCreateItemWithZeroPrice() {
        // Arrange
        Item zeroItem = new Item();
        zeroItem.setId("item-zero");
        zeroItem.setName("Free Item");
        zeroItem.setPrice(0.0);
        zeroItem.setStock(10);

        when(itemRepository.save(any(Item.class))).thenReturn(zeroItem);

        // Act
        Item result = itemService.create(zeroItem);

        // Assert
        assertNotNull(result);
        assertEquals(0.0, result.getPrice());
        verify(itemRepository, times(1)).save(zeroItem);
    }

    @Test
    void testCreateItemWithZeroStock() {
        // Arrange
        Item zeroStockItem = new Item();
        zeroStockItem.setId("item-nostock");
        zeroStockItem.setName("Out of Stock Item");
        zeroStockItem.setPrice(100.0);
        zeroStockItem.setStock(0);

        when(itemRepository.save(any(Item.class))).thenReturn(zeroStockItem);

        // Act
        Item result = itemService.create(zeroStockItem);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getStock());
        verify(itemRepository, times(1)).save(zeroStockItem);
    }

    @Test
    void testCreateItemWithNegativeStock() {
        // Arrange
        Item negativeStockItem = new Item();
        negativeStockItem.setId("item-negative");
        negativeStockItem.setName("Negative Stock Item");
        negativeStockItem.setPrice(100.0);
        negativeStockItem.setStock(-5);

        when(itemRepository.save(any(Item.class))).thenReturn(negativeStockItem);

        // Act & Assert - Should handle gracefully
        assertDoesNotThrow(() -> itemService.create(negativeStockItem));
    }

    @Test
    void testCreateItemWithNullName() {
        // Arrange
        Item nullNameItem = new Item();
        nullNameItem.setId("item-null");
        nullNameItem.setName(null);
        nullNameItem.setPrice(100.0);
        nullNameItem.setStock(10);

        when(itemRepository.save(any(Item.class))).thenReturn(nullNameItem);

        // Act
        Item result = itemService.create(nullNameItem);

        // Assert
        assertNotNull(result);
        assertNull(result.getName());
    }

    // ============ Item Retrieval Tests ============

    @Test
    void testGetAllItems() {
        // Arrange
        List<Item> items = Arrays.asList(testItem, testItem2);
        when(itemRepository.findAll()).thenReturn(items);

        // Act
        List<Item> result = itemService.getAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("item-1", result.get(0).getId());
        assertEquals("item-2", result.get(1).getId());

        // Verify
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void testGetAllItemsEmptyList() {
        // Arrange
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Item> result = itemService.getAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.size());
    }

    @Test
    void testGetItemByIdSuccess() {
        // Arrange
        when(itemRepository.findById("item-1")).thenReturn(Optional.of(testItem));

        // Act
        Item result = itemService.getById("item-1");

        // Assert
        assertNotNull(result);
        assertEquals("item-1", result.getId());
        assertEquals("Laptop", result.getName());
        verify(itemRepository, times(1)).findById("item-1");
    }

    @Test
    void testGetItemByIdNotFound() {
        // Arrange
        when(itemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act
        Item result = itemService.getById("nonexistent");

        // Assert
        assertNull(result);
        verify(itemRepository, times(1)).findById("nonexistent");
    }

    @Test
    void testGetItemByIdWithNullId() {
        // Arrange
        when(itemRepository.findById(null)).thenReturn(Optional.empty());

        // Act
        Item result = itemService.getById(null);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetItemByIdWithEmptyString() {
        // Arrange
        when(itemRepository.findById("")).thenReturn(Optional.empty());

        // Act
        Item result = itemService.getById("");

        // Assert
        assertNull(result);
    }

    // ============ Stock Deduction Tests ============

    @Test
    void testDeductStockSuccess() {
        // Arrange
        Item itemWithStock = new Item();
        itemWithStock.setId("item-1");
        itemWithStock.setName("Laptop");
        itemWithStock.setStock(50);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(itemWithStock));
        when(itemRepository.save(any(Item.class))).thenReturn(itemWithStock);

        // Act
        Item result = itemService.deductStock("item-1", 10);

        // Assert
        assertNotNull(result);
        assertEquals(40, itemWithStock.getStock());
        verify(itemRepository, times(1)).findById("item-1");
        verify(itemRepository, times(1)).save(itemWithStock);
    }

    @Test
    void testDeductStockExactAmount() {
        // Arrange
        Item itemWithStock = new Item();
        itemWithStock.setId("item-1");
        itemWithStock.setStock(10);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(itemWithStock));
        when(itemRepository.save(any(Item.class))).thenReturn(itemWithStock);

        // Act
        Item result = itemService.deductStock("item-1", 10);

        // Assert
        assertNotNull(result);
        assertEquals(0, itemWithStock.getStock());
    }

    @Test
    void testDeductStockInsufficientStock() {
        // Arrange
        Item itemWithLowStock = new Item();
        itemWithLowStock.setId("item-1");
        itemWithLowStock.setStock(5);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(itemWithLowStock));

        // Act & Assert
        Exception exception = assertThrows(
            Exception.class,
            () -> itemService.deductStock("item-1", 10)
        );

        assertTrue(exception.getMessage().contains("Insufficient") || exception.getMessage().contains("stock"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void testDeductStockZeroQuantity() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(50);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act
        Item result = itemService.deductStock("item-1", 0);

        // Assert
        assertNotNull(result);
        assertEquals(50, item.getStock());
    }

    @Test
    void testDeductStockNegativeQuantity() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(50);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act & Assert - Negative quantities should be handled
        assertDoesNotThrow(() -> itemService.deductStock("item-1", -5));
    }

    @Test
    void testDeductStockItemNotFound() {
        // Arrange
        when(itemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> itemService.deductStock("nonexistent", 5)
        );

        verify(itemRepository, times(1)).findById("nonexistent");
    }

    // ============ Item Reservation Tests ============

    @Test
    void testReserveItemSuccess() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(100);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act
        Item result = itemService.reserveItem("item-1", 20);

        // Assert
        assertNotNull(result);
        assertEquals(80, item.getStock());
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    void testReserveItemInsufficientStock() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(5);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));

        // Act & Assert
        Exception exception = assertThrows(
            Exception.class,
            () -> itemService.reserveItem("item-1", 10)
        );

        assertTrue(exception.getMessage().contains("stock") || exception.getMessage().contains("Not enough"));
        verify(itemRepository, never()).save(any());
    }

    @Test
    void testReserveItemZeroQuantity() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(50);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act
        Item result = itemService.reserveItem("item-1", 0);

        // Assert
        assertNotNull(result);
    }

    // ============ Item Restocking Tests ============

    @Test
    void testRestockItemSuccess() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(20);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act
        Item result = itemService.restockItem("item-1", 30);

        // Assert
        assertNotNull(result);
        assertEquals(50, item.getStock());
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    void testRestockItemZeroQuantity() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(50);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act
        Item result = itemService.restockItem("item-1", 0);

        // Assert
        assertNotNull(result);
        assertEquals(50, item.getStock());
    }

    @Test
    void testRestockItemLargeQuantity() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(0);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act
        Item result = itemService.restockItem("item-1", 1000);

        // Assert
        assertNotNull(result);
        assertEquals(1000, item.getStock());
    }

    @Test
    void testRestockItemNotFound() {
        // Arrange
        when(itemRepository.findById("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
            Exception.class,
            () -> itemService.restockItem("nonexistent", 10)
        );

        verify(itemRepository, times(1)).findById("nonexistent");
    }

    // ============ Multiple Operations Tests ============

    @Test
    void testMultipleDeductionsOnSameItem() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(100);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act
        itemService.deductStock("item-1", 25);  // 75 remaining
        itemService.deductStock("item-1", 25);  // 50 remaining
        itemService.deductStock("item-1", 25);  // 25 remaining

        // Assert
        assertEquals(25, item.getStock());
        verify(itemRepository, times(3)).save(any(Item.class));
    }

    @Test
    void testReserveAndRestockCycle() {
        // Arrange
        Item item = new Item();
        item.setId("item-1");
        item.setStock(100);

        when(itemRepository.findById("item-1")).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);

        // Act - Reserve
        itemService.reserveItem("item-1", 30);
        assertEquals(70, item.getStock());

        // Act - Restock
        itemService.restockItem("item-1", 30);
        assertEquals(100, item.getStock());

        // Assert
        verify(itemRepository, times(2)).save(any(Item.class));
    }

    @Test
    void testCreateMultipleItems() {
        // Arrange
        when(itemRepository.save(any(Item.class))).thenReturn(testItem, testItem2);

        // Act
        Item result1 = itemService.create(testItem);
        Item result2 = itemService.create(testItem2);

        // Assert
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("item-1", result1.getId());
        assertEquals("item-2", result2.getId());
        verify(itemRepository, times(2)).save(any(Item.class));
    }
}
