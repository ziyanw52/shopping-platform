package com.ziyan.item.controller;

import com.ziyan.item.entity.Item;
import com.ziyan.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    public Item create(@RequestBody Item item) {
        return itemService.create(item);
    }

    @GetMapping
    public List<Item> getAll() {
        return itemService.getAll();
    }

    @GetMapping("/{id}")
    public Item getById(@PathVariable String id) {
        return itemService.getById(id);
    }

    @PostMapping("/{id}/deduct")
    public Item deductStock(@PathVariable String id,
                            @RequestParam Integer quantity) {
        return itemService.deductStock(id, quantity);
    }

    @PostMapping("/{id}/reserve")
    public Item reserveItem(@PathVariable String id,
                            @RequestParam Integer quantity) {
        return itemService.reserveItem(id, quantity);
    }

    @PostMapping("/{id}/restock")
    public Item restockItem(@PathVariable String id,
                            @RequestParam Integer quantity) {
        return itemService.restockItem(id, quantity);
    }
}