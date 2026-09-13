package com.example.ordering.application;

import com.example.ordering.domain.Money;
import com.example.ordering.domain.Product;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Ban hien thuc bang HashMap, du de chay demo ma khong can database. */
public class InMemoryProductRepository implements ProductRepository {

    private final Map<String, Product> data = new LinkedHashMap<>();

    public InMemoryProductRepository() {
        save(new Product("SKU-KEYBOARD", "Ban phim co Keychron K2", Money.of("129.00"), 10));
        save(new Product("SKU-MOUSE", "Chuot Logitech MX Master", Money.of("99.50"), 5));
        save(new Product("SKU-CABLE", "Cap USB-C 2m", Money.of("12.00"), 100));
        save(new Product("SKU-MONITOR", "Man hinh Dell 27 inch", Money.of("450.00"), 2));
        save(new Product("SKU-HEADSET", "Tai nghe Sony WH-1000XM5", Money.of("348.00"), 0));
    }

    @Override
    public Optional<Product> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public void save(Product product) {
        data.put(product.getId(), product);
    }
}
