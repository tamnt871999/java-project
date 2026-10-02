package com.example.ordering.adapter.out.persistence;

import com.example.ordering.adapter.out.persistence.OrderJpaEntity.ItemEmbeddable;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderItem;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    OrderRepositoryAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity saved = jpaRepository.save(toEntity(order));
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(Long orderId) {
        return jpaRepository.findById(orderId).map(OrderRepositoryAdapter::toDomain);
    }

    private static OrderJpaEntity toEntity(Order order) {
        List<ItemEmbeddable> items = order.items().stream()
                .map(item -> new ItemEmbeddable(item.productId(), item.quantity(), item.unitPrice()))
                .toList();
        return new OrderJpaEntity(order.customerId(), order.placedAt(), items);
    }

    private static Order toDomain(OrderJpaEntity entity) {
        List<OrderItem> items = entity.getItems().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return Order.rehydrate(entity.getId(), entity.getCustomerId(), items, entity.getPlacedAt());
    }
}
