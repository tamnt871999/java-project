package com.example.logistics.core.domain;

import java.util.Locale;
import java.util.Objects;

/**
 * Ma nha van chuyen (GHN, GHTK, ...).
 *
 * VI SAO KHONG DUNG enum?
 * Enum la mot danh sach dong: muon them doi tac thu ba (ViettelPost) thi phai
 * SUA lai file enum - dung mot lan nua vao dung cho ma nguyen ly OCP cam sua.
 * Voi value object nhu duoi day, them doi tac = them MOT file adapter moi va
 * mot dong dang ky o Composition Root, khong dong vao bat ky file cu nao.
 *
 * Luu y ranh gioi: "GHN" o day la ten DOI TAC KINH DOANH, khong phai chi tiet
 * cong nghe. Loi duoc phep biet minh dang lam viec voi doi tac nao; cai loi
 * KHONG duoc biet la doi tac do goi bang SDK, REST hay SOAP.
 */
public final class CarrierCode {

    public static final CarrierCode GHN = of("GHN", "GiaoHangNhanh");
    public static final CarrierCode GHTK = of("GHTK", "GiaoHangTietKiem");

    private final String value;
    private final String partnerName;

    private CarrierCode(String value, String partnerName) {
        this.value = value;
        this.partnerName = partnerName;
    }

    public static CarrierCode of(String value, String partnerName) {
        Objects.requireNonNull(value, "carrier code must not be null");
        Objects.requireNonNull(partnerName, "partner name must not be null");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new DomainException("Ma nha van chuyen khong duoc rong");
        }
        return new CarrierCode(normalized, partnerName.trim());
    }

    public String value() {
        return value;
    }

    /** Ten day du de hien thi cho khach: "GiaoHangNhanh". */
    public String partnerName() {
        return partnerName;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CarrierCode c && value.equals(c.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
