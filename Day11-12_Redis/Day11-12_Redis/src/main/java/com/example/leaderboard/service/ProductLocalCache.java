package com.example.leaderboard.service;

import com.example.leaderboard.service.ProductService.ProductView;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ProductLocalCache {

    private final Map<Long, ProductView> entries = new ConcurrentHashMap<>();

    public Optional<ProductView> get(Long id) {
        return Optional.ofNullable(entries.get(id));
    }

    public void put(ProductView product) {
        entries.put(product.id(), product);
    }

    public void evict(Long id) {
        entries.remove(id);
    }
}
