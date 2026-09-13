package com.example.logistics.infrastructure.carrier.ghn;

import java.util.Locale;
import java.util.Set;

/**
 * GIA LAP SDK cua GiaoHangNhanh (thu vien do doi tac phat hanh).
 *
 * Coi day la file .jar tai ve tu GHN: ta KHONG duoc sua no, va no viet theo
 * phong cach rieng cua no - dung field public thay vi getter, dung int thay
 * vi BigDecimal, tra ve ma loi so thay vi nem ngoai le, do don vi la GRAM,
 * va bat buoc phai co token khi khoi tao.
 *
 * Chinh vi ta khong the sua noi nhung thu nay ma he thong moi can Adapter:
 * ai do phai dich giua "the gioi cua GHN" va "the gioi cua Domain".
 */
public final class GhnShippingSdk {

    /** Dich vu chuan cua GHN (gia lap ma dich vu that). */
    public static final int SERVICE_TYPE_STANDARD = 2;

    private static final int BASE_FEE = 22_000;
    private static final int BLOCK_GRAM = 500;
    private static final int FEE_PER_BLOCK = 5_000;
    private static final int REMOTE_SURCHARGE = 18_000;

    /** Cac quan huyen GHN co kho trung chuyen - giao nhanh, khong phu phi vung xa. */
    private static final Set<String> HUB_CITIES = Set.of(
            "ha noi", "ho chi minh", "da nang", "hai phong", "can tho",
            "binh duong", "dong nai");

    private final String token;

    public GhnShippingSdk(String token) {
        if (token == null || token.isBlank()) {
            throw new GhnApiException("Missing GHN API token");
        }
        this.token = token;
    }

    /** Tham so goi API - kieu du lieu do GHN dinh nghia. */
    public static final class FeeRequest {
        public String toDistrictName;
        public int weightGram;
        public int serviceTypeId;
    }

    /** Phan hoi API - GHN bao loi bang truong code chu khong nem ngoai le. */
    public static final class FeeResponse {
        public int code;
        public String message;
        public int total;
        public int expectedDeliveryDay;
        public String serviceName;
    }

    public FeeResponse calculateFee(FeeRequest request) {
        if (request == null || request.toDistrictName == null) {
            throw new GhnApiException("Invalid request: toDistrictName is required");
        }
        FeeResponse response = new FeeResponse();
        if (request.weightGram <= 0) {
            response.code = 400;
            response.message = "weight must be greater than 0";
            return response;
        }
        if (request.serviceTypeId != SERVICE_TYPE_STANDARD) {
            response.code = 400;
            response.message = "unsupported service_type_id: " + request.serviceTypeId;
            return response;
        }

        boolean hub = HUB_CITIES.contains(request.toDistrictName.toLowerCase(Locale.ROOT));
        int blocks = Math.max(0, (request.weightGram - BLOCK_GRAM + BLOCK_GRAM - 1) / BLOCK_GRAM);

        response.code = 200;
        response.message = "OK";
        response.total = BASE_FEE + blocks * FEE_PER_BLOCK + (hub ? 0 : REMOTE_SURCHARGE);
        response.expectedDeliveryDay = hub ? 1 : 3;
        response.serviceName = "GHN Standard";
        return response;
    }

    public String token() {
        return token;
    }
}
