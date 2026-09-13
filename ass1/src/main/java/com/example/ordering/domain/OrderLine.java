package com.example.ordering.domain;

import java.util.Objects;

/**
 * MODEL - mot dong trong don hang.
 *
 * Luu lai TEN va DON GIA tai thoi diem mua. Neu mai mot san pham doi gia,
 * don hang cu van giu nguyen gia da chot.
 */
public class OrderLine {

    private final String productId;
    private final String productName;
    private final Money unitPrice;
    private final int quantity;

    public OrderLine(String productId, String productName, Money unitPrice, int quantity) {
        this.productId = Objects.requireNonNull(productId, "productId must not be null");
        this.productName = Objects.requireNonNull(productName, "productName must not be null");
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice must not be null");
        if (quantity <= 0) {
            throw new DomainException("So luong phai lon hon 0, nhan duoc: " + quantity);
        }
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    /** Thanh tien = don gia x so luong. */
    public Money getLineTotal() {
        return unitPrice.times(quantity);
    }

    @Override
    public String toString() {
        return productName + " x" + quantity + " = " + getLineTotal();
    }
}
