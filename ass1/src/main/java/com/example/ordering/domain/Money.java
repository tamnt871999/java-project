package com.example.ordering.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * VALUE OBJECT tien te - bat bien, so sanh theo gia tri.
 *
 * Khong bao gio dung double cho tien: 0.1 + 0.2 voi double ra
 * 0.30000000000000004. Moi phep tinh deu qua BigDecimal, lam tron 2 chu so.
 *
 * Luu y: toString() tra ve chuoi KY THUAT ("1234.50"), khong them ky hieu tien
 * te hay dau phan cach. Dinh dang hien thi phu thuoc ngon ngu va vung mien cua
 * nguoi xem nen thuoc ve client, khong thuoc ve Entities.
 */
public final class Money implements Comparable<Money> {

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new DomainException("So tien khong duoc am: " + amount);
        }
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
        return other instanceof Money money && amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
