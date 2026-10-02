package com.example.ordering.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public interface PlaceOrderUseCase {

    PlaceOrderResult placeOrder(PlaceOrderCommand command);

    record PlaceOrderCommand(String customerId, List<Item> items) {

        public record Item(String productId, int quantity, BigDecimal unitPrice) {
        }
    }

    record PlaceOrderResult(Long orderId, BigDecimal total) {
    }
}
