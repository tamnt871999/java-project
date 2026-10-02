package com.example.ordering.application.port.in;

import java.math.BigDecimal;
import java.util.List;

public interface GetOrderUseCase {

    OrderView getOrder(Long orderId);

    record OrderView(Long orderId,
                     String customerId,
                     String placedAt,
                     List<Line> items,
                     BigDecimal total) {

        public record Line(String productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
        }
    }

    class OrderNotFoundException extends RuntimeException {

        public OrderNotFoundException(Long orderId) {
            super("Khong tim thay don hang: " + orderId);
        }
    }
}
