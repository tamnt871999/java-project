package com.example.ordering.application;

import com.example.ordering.domain.Order;

import java.util.List;
import java.util.Optional;

/** Repository - luu va doc don hang. */
public interface OrderRepository {

    void save(Order order);

    Optional<Order> findById(String id);

    List<Order> findAll();

    String nextId();
}
