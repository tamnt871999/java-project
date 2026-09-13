package com.example.logistics.core.factory;

import com.example.logistics.core.domain.CarrierCode;
import com.example.logistics.core.domain.City;
import com.example.logistics.core.domain.DomainException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * LUAT DINH TUYEN: tinh / thanh nao thi giao cho doi tac nao.
 *
 * Day la QUYET DINH KINH DOANH ("noi thanh cac thanh pho lon uu tien GHN vi
 * giao trong ngay; cac tinh con lai dung GHTK vi re hon"), khong phai chi tiet
 * ky thuat. Vi vay no thuoc ve Application Core, dat canh Factory.
 *
 * Bang luat duoc TRUYEN VAO chu khong hard-code trong than ham: doi chinh sach
 * gia cua doi tac thi chi sua bang du lieu o Composition Root hoac nap tu file
 * cau hinh, khong phai sua logic.
 */
public final class CarrierRoutingPolicy {

    /** key = City.key() da chuan hoa. */
    private final Map<String, CarrierCode> routes;
    private final CarrierCode fallback;

    private CarrierRoutingPolicy(Map<String, CarrierCode> routes, CarrierCode fallback) {
        this.routes = Collections.unmodifiableMap(new LinkedHashMap<>(routes));
        this.fallback = Objects.requireNonNull(fallback, "fallback carrier must not be null");
    }

    /**
     * Chinh sach mac dinh cua de bai:
     *   - 5 thanh pho truc thuoc trung uong -> GHN (co hub, giao nhanh).
     *   - Cac tinh con lai                  -> GHTK (toi uu chi phi).
     */
    public static CarrierRoutingPolicy defaultPolicy() {
        return builder()
                .route("Ha Noi", CarrierCode.GHN)
                .route("Ho Chi Minh", CarrierCode.GHN)
                .route("Da Nang", CarrierCode.GHN)
                .route("Hai Phong", CarrierCode.GHN)
                .route("Can Tho", CarrierCode.GHN)
                .fallback(CarrierCode.GHTK)
                .build();
    }

    /** Tra ve doi tac phu trach thanh pho nay; khong co luat rieng thi dung fallback. */
    public CarrierCode carrierFor(City destination) {
        Objects.requireNonNull(destination, "destination must not be null");
        return routes.getOrDefault(destination.key(), fallback);
    }

    /** Ban sao chi doc cua bang luat - phuc vu hien thi / kiem thu. */
    public Map<String, CarrierCode> routes() {
        return routes;
    }

    public CarrierCode fallback() {
        return fallback;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<String, CarrierCode> routes = new LinkedHashMap<>();
        private CarrierCode fallback;

        public Builder route(String city, CarrierCode carrier) {
            Objects.requireNonNull(carrier, "carrier must not be null");
            // Chuan hoa ngay khi nap luat, de "TP. Ho Chi Minh" va "ho chi minh"
            // khong tao ra hai dong luat khac nhau.
            routes.put(City.of(city).key(), carrier);
            return this;
        }

        public Builder fallback(CarrierCode carrier) {
            this.fallback = carrier;
            return this;
        }

        public CarrierRoutingPolicy build() {
            if (fallback == null) {
                throw new DomainException("Phai khai bao nha van chuyen mac dinh (fallback)");
            }
            return new CarrierRoutingPolicy(routes, fallback);
        }
    }
}
