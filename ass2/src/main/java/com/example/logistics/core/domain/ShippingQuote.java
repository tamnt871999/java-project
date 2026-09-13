package com.example.logistics.core.domain;

import java.util.Objects;

/**
 * Ket qua bao gia tra ve cho tang ngoai.
 *
 * Moi Adapter nhan mot payload rieng (JSON cua GHTK, object cua GHN SDK) roi
 * dich ve DUNG kieu nay. Nho the Use Case chi phai hieu mot dang ket qua duy
 * nhat du he thong co bao nhieu doi tac di nua.
 */
public class ShippingQuote {

    private final CarrierCode carrier;
    private final Money fee;
    private final int estimatedDays;
    private final String note;

    /**
     * @param carrier       nha van chuyen da duoc chon
     * @param fee           cuoc phi (VND)
     * @param estimatedDays so ngay du kien giao
     * @param note          ghi chu cua doi tac (goi dich vu, vung xa, ...)
     */
    public ShippingQuote(CarrierCode carrier, Money fee, int estimatedDays, String note) {
        this.carrier = Objects.requireNonNull(carrier, "carrier must not be null");
        this.fee = Objects.requireNonNull(fee, "fee must not be null");
        if (estimatedDays <= 0) {
            throw new DomainException("So ngay giao du kien phai lon hon 0: " + estimatedDays);
        }
        this.estimatedDays = estimatedDays;
        this.note = note == null ? "" : note.trim();
    }

    public CarrierCode getCarrier() {
        return carrier;
    }

    public Money getFee() {
        return fee;
    }

    public int getEstimatedDays() {
        return estimatedDays;
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return carrier + " " + fee + " (" + estimatedDays + " ngay)";
    }
}
