package com.example.ordering.application.port.out;

import com.example.ordering.domain.Order;

import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long orderId);
}
