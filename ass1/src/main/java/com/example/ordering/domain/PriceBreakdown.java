package com.example.ordering.domain;

import java.util.Objects;

/**
 * Ket qua tinh gia: tach tung phan de hoa don minh bach va de kiem thu rieng le.
 *
 * Day la gia tri ma OrderPricingService tra ve o buoc calculateTotal(items)
 * trong sequence diagram.
 */
public record PriceBreakdown(Money subtotal, Money discount, Money shippingFee, Money total) {

    public PriceBreakdown {
        Objects.requireNonNull(subtotal, "subtotal must not be null");
        Objects.requireNonNull(discount, "discount must not be null");
        Objects.requireNonNull(shippingFee, "shippingFee must not be null");
        Objects.requireNonNull(total, "total must not be null");
    }
}
