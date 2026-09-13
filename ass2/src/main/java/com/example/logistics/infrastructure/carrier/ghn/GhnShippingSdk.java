package com.example.logistics.infrastructure.carrier.ghn;

import java.util.Set;

/**
 * GIA LAP SDK cua GiaoHangNhanh (thu vien do doi tac phat hanh).
 *
 * Coi day la file .jar tai ve tu GHN: ta KHONG duoc sua no, va no viet theo
 * phong cach rieng cua no - dung field public thay vi getter, dung int thay
 * vi BigDecimal, tra ve ma loi so thay vi nem ngoai le, do don vi la GRAM,
 * va bat buoc phai co token khi khoi tao.
 *
 * QUAN TRONG - dia chi la MA SO, khong phai ten: GHN dinh danh dia ban bang
 * district_id (so nguyen trong danh muc rieng cua ho), y het API that. No
 * khong he biet "ha noi" hay "Hà Nội" nghia la gi. Viec tra ten thanh pho ra
 * district_id la trach nhiem cua GhnCarrierAdapter.
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

    /** district_id co kho trung chuyen - giao nhanh, khong phu phi vung xa. */
    private static final Set<Integer> HUB_DISTRICT_IDS = Set.of(
            1442, 1454, 1526, 1574, 1602, 1630, 1658);

    private final String token;

    public GhnShippingSdk(String token) {
        if (token == null || token.isBlank()) {
            throw new GhnApiException("Missing GHN API token");
        }
        this.token = token;
    }

    /** Tham so goi API - kieu du lieu do GHN dinh nghia. */
    public static final class FeeRequest {
        public int toDistrictId;
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
        if (request == null) {
            throw new GhnApiException("Invalid request: body is required");
        }
        FeeResponse response = new FeeResponse();
        if (request.toDistrictId <= 0) {
            response.code = 400;
            response.message = "to_district_id is required";
            return response;
        }
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

        boolean hub = HUB_DISTRICT_IDS.contains(request.toDistrictId);
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
