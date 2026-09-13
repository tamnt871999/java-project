package com.example.logistics.core.port.dto;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.DomainException;
import com.example.logistics.core.domain.Money;

import java.util.Objects;

/**
 * Ket qua bao gia tra ve cho tang ngoai.
 *
 * Moi Adapter nhan mot payload rieng (JSON cua GHTK, object cua GHN SDK) roi
 * dich ve DUNG kieu nay. Nho the Use Case chi phai hieu mot dang ket qua duy
 * nhat du he thong co bao nhieu doi tac di nua.
 *
 * VE TRUONG serviceName: day la ten GOI DICH VU cua doi tac ("GHN Standard",
 * "GHTK Tiet Kiem") - mot du kien nghiep vu that, vi khach hang can biet minh
 * dang mua goi nao va no la can cu khi doi soat cuoc.
 *
 * Truoc kia truong nay ten la "note" va nhan chuoi tu do do Adapter ghep san,
 * kieu "GHTK Tiet Kiem (72h)". Do la MOT LOI RANH GIOI: phan "(72h)" thuc chat
 * la cach TRINH BAY lai thong tin ma estimatedDays da mang, nen loi vo tinh
 * chua san mot manh giao dien. Doi ten va siet rang buoc de truong nay chi
 * dung mot viec: dinh danh goi dich vu.
 */
public class ShippingQuote {

    private final CarrierCode carrier;
    private final Money fee;
    private final int estimatedDays;
    private final String serviceName;

    /**
     * @param carrier       nha van chuyen da duoc chon
     * @param fee           cuoc phi (VND)
     * @param estimatedDays so ngay du kien giao
     * @param serviceName   ten goi dich vu cua doi tac, vi du "GHN Standard"
     */
    public ShippingQuote(CarrierCode carrier, Money fee, int estimatedDays, String serviceName) {
        this.carrier = Objects.requireNonNull(carrier, "carrier must not be null");
        this.fee = Objects.requireNonNull(fee, "fee must not be null");
        if (estimatedDays <= 0) {
            throw new DomainException("So ngay giao du kien phai lon hon 0: " + estimatedDays);
        }
        this.estimatedDays = estimatedDays;
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        if (serviceName.isBlank()) {
            throw new DomainException("Bao gia phai kem ten goi dich vu");
        }
        this.serviceName = serviceName.trim();
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

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return carrier + " " + serviceName + " " + fee + " (" + estimatedDays + " ngay)";
    }
}
