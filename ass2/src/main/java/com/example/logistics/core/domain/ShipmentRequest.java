package com.example.logistics.core.domain;

import java.util.Objects;

/**
 * Yeu cau bao gia: giao MOT kien hang nang bao nhieu, di tinh / thanh nao.
 *
 * Day la ngon ngu cua LOI. Khong co field nao mang mui cong nghe hay mui
 * doi tac (khong co token, khong co district_id cua GHN, khong co pick_province
 * cua GHTK). Nho vay doi doi tac khong lam thay doi kieu du lieu nay.
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
