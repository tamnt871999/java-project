package com.example.ordering.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * MODEL - gia tri tien te.
 *
 * Dung BigDecimal chu khong dung double: 0.1 + 0.2 voi double se ra
 * 0.30000000000000004, sai so nay khong chap nhan duoc voi tien.
 */
public class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public BigDecimal amount() {
        return amount;
    }

    public Money plus(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(this.amount.subtract(other.amount));
    }

    public Money times(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money percent(int percent) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
    }

    public boolean isAtLeast(Money other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(Money other) {
        return this.amount.compareTo(other.amount);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Money m && amount.compareTo(m.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    /**
     * Chuoi ky thuat thuan tuy: 1234.5 -> "1234.50".
     *
     * Domain KHONG dinh dang tien de hien thi (khong them $, khong them dau
     * phay ngan cach). Viec do phu thuoc ngon ngu va vung mien cua nguoi xem,
     * nen thuoc ve client hoac tang adapter, khong thuoc ve Model.
     */
    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
