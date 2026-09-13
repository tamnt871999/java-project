package com.example.ordering.application;

import com.example.ordering.domain.Order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Ban hien thuc bang HashMap kem bo dem sinh ma don. */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> data = new LinkedHashMap<>();
    private final AtomicInteger counter = new AtomicInteger(1000);

    @Override
    public void save(Order order) {
        data.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(data.get(id));
    }

    @Override
    public List<Order> findAll() {
        List<Order> orders = new ArrayList<>(data.values());
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return orders;
    }

    @Override
    public String nextId() {
        return "ORD-" + counter.incrementAndGet();
    }
}
