package com.example.ordering.domain;

import java.util.Objects;

/**
 * ENTITY con nam trong aggregate Order.
 *
 * Chup lai don gia tai thoi diem dat hang: sau nay san pham co doi gia thi don
 * hang cu van giu nguyen gia da chot.
 */
public record OrderItem(ProductId productId, Quantity quantity, Money unitPrice) {

    public OrderItem {
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(quantity, "quantity must not be null");
        Objects.requireNonNull(unitPrice, "unitPrice must not be null");
    }

    public static OrderItem of(String productId, int quantity, String unitPrice) {
        return new OrderItem(ProductId.of(productId), Quantity.of(quantity), Money.of(unitPrice));
    }

    /** Thanh tien cua mot dong = don gia x so luong. */
    public Money lineTotal() {
        return unitPrice.times(quantity.value());
    }

    @Override
    public String toString() {
        return "%s x%s = %s".formatted(productId, quantity, lineTotal());
    }
}
