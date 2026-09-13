package com.example.logistics.core.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Gia tri tien te (VND).
 *
 * Dung BigDecimal chu khong dung double: sai so dau phay dong khong chap
 * nhan duoc voi tien. VND khong co phan le nen scale = 0.
 *
 * KHONG co ham dinh dang "25.000 d" o day: dinh dang phu thuoc ngon ngu va
 * vung mien cua nguoi xem, do la viec cua tang ngoai (CLI / REST / UI).
 */
public class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new DomainException("So tien khong the am: " + amount.toPlainString());
        }
        this.amount = amount.setScale(0, RoundingMode.HALF_UP);
    }

    public static Money ofVnd(long vnd) {
        return new Money(BigDecimal.valueOf(vnd));
    }

    public BigDecimal amount() {
        return amount;
    }

    public long toVnd() {
        return amount.longValueExact();
    }

    public Money plus(Money other) {
        return new Money(this.amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(this.amount.subtract(other.amount));
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

    /** Chuoi ky thuat thuan tuy: 25000 -> "25000". */
    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
