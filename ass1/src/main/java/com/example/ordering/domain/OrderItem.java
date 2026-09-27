package com.example.ordering.domain;

import java.math.BigDecimal;

public record OrderItem(String productId, int quantity, BigDecimal unitPrice) {

    public OrderItem {
        if (productId == null || productId.isBlank()) {
            throw new DomainException("Thieu productId");
        }
        if (quantity < 1) {
            throw new DomainException("So luong phai lon hon 0, nhan duoc " + quantity);
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new DomainException("Don gia khong hop le: " + unitPrice);
        }
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
