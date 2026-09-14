package com.example.ordering.application.usecase;

import com.example.ordering.application.port.in.FindOrderUseCase;
import com.example.ordering.application.port.in.PlaceOrderResult;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.OrderId;

import java.util.Objects;

/** INTERACTOR cho luong doc - mong nhu to giay, va dung ra thi no nen nhu vay. */
public class FindOrderService implements FindOrderUseCase {

    private final OrderRepository orderRepository;

    public FindOrderService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    @Override
    public PlaceOrderResult findById(String orderId) {
        return orderRepository.findById(OrderId.of(orderId))
                .map(PlaceOrderService::toResult)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
