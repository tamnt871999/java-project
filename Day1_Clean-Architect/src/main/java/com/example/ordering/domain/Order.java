package com.example.ordering.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Order {

    private final Long id;
    private final String customerId;
    private final List<OrderItem> items;
    private final Instant placedAt;

    private Order(Long id, String customerId, List<OrderItem> items, Instant placedAt) {
        this.id = id;
        this.customerId = customerId;
        this.items = List.copyOf(items);
        this.placedAt = placedAt;
    }

    public static Order place(String customerId, List<OrderItem> items, Instant placedAt) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainException("Thieu customerId");
        }
        if (items == null || items.isEmpty()) {
            throw new DomainException("Don hang phai co it nhat mot dong hang");
        }
        requireNoDuplicateProduct(items);
        return new Order(null, customerId, items, placedAt);
    }

    public static Order rehydrate(Long id, String customerId, List<OrderItem> items, Instant placedAt) {
        return new Order(id, customerId, items, placedAt);
    }

    public BigDecimal total() {
        return items.stream()
                .map(OrderItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void requireNoDuplicateProduct(List<OrderItem> items) {
        Set<String> seen = new HashSet<>();
        for (OrderItem item : items) {
            if (!seen.add(item.productId())) {
                throw new DomainException("San pham bi lap lai trong don hang: " + item.productId());
            }
        }
    }

    public Long id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public List<OrderItem> items() {
        return items;
    }

    public Instant placedAt() {
        return placedAt;
    }
}
