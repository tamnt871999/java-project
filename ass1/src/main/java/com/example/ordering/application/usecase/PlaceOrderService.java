package com.example.ordering.application.usecase;

import com.example.ordering.application.port.in.PlaceOrderUseCase;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderItem;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderRepository orderRepository;

    PlaceOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        List<OrderItem> items = command.items().stream()
                .map(item -> new OrderItem(item.productId(), item.quantity(), item.unitPrice()))
                .toList();

        Order order = Order.place(command.customerId(), items, Instant.now());
        Order saved = orderRepository.save(order);

        return new PlaceOrderResult(saved.id(), saved.total());
    }
}
