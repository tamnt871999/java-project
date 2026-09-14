package com.example.wallet.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * VALUE OBJECT tien te - bat bien, so sanh theo gia tri.
 *
 * Vi sao khong de balance la BigDecimal tran?
 *
 * Vi "so tien khong duoc am" la mot luat, va luat thi phai co MOT cho de thuc
 * thi. De BigDecimal tran thi luat do phai duoc lap lai o moi noi cham vao
 * tien - va chi can quen mot cho la thung, dung nhu ma nguon cu da thung.
 *
 * Khong bao gio dung double cho tien: 0.1 + 0.2 voi double ra
 * 0.30000000000000004. Moi phep tinh deu qua BigDecimal, lam tron 2 chu so.
 *
 * Luu y: toString() tra ve chuoi KY THUAT ("1234.50"), khong them ky hieu tien
 * te hay dau phan cach. Dinh dang hien thi phu thuoc ngon ngu va vung mien cua
 * nguoi xem nen thuoc ve tang ngoai, khong thuoc ve Entities.
 */
public final class Money implements Comparable<Money> {

    private final BigDecimal amount;

    public Money(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.signum() < 0) {
            throw new DomainException("So tien khong duoc am: " + amount.toPlainString());
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public BigDecimal amount() {
        return amount;
    }

    /**
     * Chi co phep TRU, khong co phep cong.
     *
     * Pham vi bai tap chi gom rut tien va khoa vi, nen khong viet san plus()
     * cho mot nghiep vu nap tien chua ton tai. Them ham "de sau nay dung" la
     * cach nhanh nhat de co ma chet khong ai kiem thu.
     */
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
