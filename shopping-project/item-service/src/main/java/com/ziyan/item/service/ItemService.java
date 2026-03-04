package com.ziyan.item.service;

import com.ziyan.item.entity.Item;
import com.ziyan.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Item create(Item item) {
        return itemRepository.save(item);
    }

    public List<Item> getAll() {
        return itemRepository.findAll();
    }

    public Item getById(String id) {
        return itemRepository.findById(id).orElse(null);
    }

    public Item deductStock(String id, Integer quantity) {
        Item item = itemRepository.findById(id).orElseThrow();
        if (item.getStock() < quantity) {
            throw new RuntimeException("Not enough stock");
        }
        item.setStock(item.getStock() - quantity);
        return itemRepository.save(item);
    }
}