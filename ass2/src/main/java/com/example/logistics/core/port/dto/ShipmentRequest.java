package com.example.logistics.core.port.dto;

import com.example.logistics.core.domain.City;
import com.example.logistics.core.domain.Weight;

import java.util.Objects;

/**
 * Yeu cau bao gia: giao MOT kien hang nang bao nhieu, di tinh / thanh nao.
 *
 * VI SAO NAM O port/dto CHU KHONG PHAI o domain?
 *
 * Day khong phai mot khai niem nghiep vu ton tai doc lap nhu City hay Money -
 * no la HOP DONG DU LIEU tai ranh gioi cua loi, do Use Case dinh nghia de noi
 * chuyen voi ben ngoai. Neu de chung voi domain thi som muon nhu cau cua tang
 * web se bo nguoc vao tang trong cung: mot ngay dep troi co nguoi them
 * @JsonProperty hoac mot field chi de hien thi vao day, va entity bi keo theo
 * nhip thay doi cua REST API.
 *
 * Tach ra roi thi domain chi con gom value object thuan (City, Weight, Money,
 * CarrierCode) - thu on dinh nhat, thay doi cham nhat.
 *
 * Luu y ve ngon ngu: khong co field nao mang mui cong nghe hay mui doi tac
 * (khong token, khong district_id cua GHN, khong pick_province cua GHTK).
 * Nho vay doi doi tac khong lam thay doi kieu du lieu nay.
 */
public class ShipmentRequest {

    private final City destination;
    private final Weight weight;

    public ShipmentRequest(City destination, Weight weight) {
        this.destination = Objects.requireNonNull(destination, "destination must not be null");
        this.weight = Objects.requireNonNull(weight, "weight must not be null");
    }

    /** Loi tat cho tang ngoai: ShipmentRequest.of("Ha Noi", 1200). */
    public static ShipmentRequest of(String city, long grams) {
        return new ShipmentRequest(City.of(city), Weight.ofGrams(grams));
    }

    public City getDestination() {
        return destination;
    }

    public Weight getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return weight + " -> " + destination;
    }
}
