package com.example.ordering;

import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderId;
import com.example.ordering.domain.OrderItem;
import com.example.ordering.domain.OrderPricingService;
import com.example.ordering.domain.PriceBreakdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bo ghi hinh cac lifeline trong sequence diagram.
 *
 * Y tuong: boc cac cong tac (domain service, outbound port) bang lop vo ghi lai
 * TEN va THU TU moi loi goi. Nho vay ta kiem chung duoc rang luong chay that su
 * dien ra dung trinh tu ma sequence diagram mo ta, chu khong chi tin vao mat
 * thuong khi doc code.
 */
final class SequenceRecorder {

    private final List<String> calls = new ArrayList<>();

    List<String> calls() {
        return List.copyOf(calls);
    }

    void record(String call) {
        calls.add(call);
    }

    /** Vo boc quanh OrderPricingService - lifeline "OrderPricingService". */
    OrderPricingService pricingService() {
        return new OrderPricingService() {
            @Override
            public PriceBreakdown calculateTotal(List<OrderItem> items) {
                record("OrderPricingService.calculateTotal");
                return super.calculateTotal(items);
            }
        };
    }

    /** Vo boc quanh outbound port - lifeline "OrderRepository" va adapter phia sau. */
    OrderRepository orderRepository(OrderRepository delegate) {
        return new OrderRepository() {
            @Override
            public Order save(Order order) {
                record("OrderRepository.save");
                return delegate.save(order);
            }

            @Override
            public Optional<Order> findById(OrderId orderId) {
                record("OrderRepository.findById");
                return delegate.findById(orderId);
            }

            @Override
            public OrderId nextOrderId() {
                record("OrderRepository.nextOrderId");
                return delegate.nextOrderId();
            }
        };
    }
}
