package com.example.ordering.application.usecase;

import com.example.ordering.application.port.in.GetOrderUseCase;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.Order;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    GetOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public OrderView getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        List<OrderView.Line> lines = order.items().stream()
                .map(item -> new OrderView.Line(
                        item.productId(), item.quantity(), item.unitPrice(), item.lineTotal()))
                .toList();

        return new OrderView(
                order.id(), order.customerId(), order.placedAt().toString(), lines, order.total());
    }
}
