package com.example.ordering;

import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderItem;
import com.example.ordering.domain.OrderPricingService;
import com.example.ordering.domain.PriceBreakdown;

import java.util.ArrayList;
import java.util.List;

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

    /** Vo boc lifeline "OrderPricingService (Domain Service / Entities)". */
    OrderPricingService pricingService() {
        return new OrderPricingService() {
            @Override
            public PriceBreakdown calculateTotal(List<OrderItem> items) {
                calls.add("OrderPricingService.calculateTotal");
                return super.calculateTotal(items);
            }
        };
    }

    /** Vo boc lifeline "OrderRepository (Outbound Port / Application)". */
    OrderRepository orderRepository(OrderRepository delegate) {
        return order -> {
            calls.add("OrderRepository.save");
            return delegate.save(order);
        };
    }
}
