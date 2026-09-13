package com.example.logistics.infrastructure.carrier.ghtk;

import java.util.Locale;
import java.util.Set;

/**
 * GIA LAP REST API cua GiaoHangTietKiem.
 *
 * Doi tac thu hai co phong cach hoan toan khac GHN - va do chinh la van de ma
 * kien truc nay phai giai quyet:
 *
 *              | GHN                    | GHTK
 *   giao tiep  | SDK Java (object)      | HTTP REST (chuoi JSON tho)
 *   trong luong| gram (int)             | kilogram (double)
 *   tien       | VND (int)              | NGHIN dong (int)
 *   thoi gian  | so ngay                | so GIO
 *   bao loi    | truong code trong body | nem ngoai le
 *
 * Neu Use Case goi thang hai thu nay thi no phai tu biet quy doi gio sang ngay
 * va nghin dong sang dong - tuc la kien thuc ha tang da chui vao loi nghiep vu.
 */
public final class GhtkRestClient {

    private static final int BASE_FEE_THOUSAND = 16;      // 16.000d cho 1kg dau
    private static final int FEE_PER_HALF_KG_THOUSAND = 4;
    private static final int REMOTE_SURCHARGE_THOUSAND = 10;
    private static final double MAX_KILOGRAMS = 20.0;

    /** Vung GHTK co buu cuc noi tinh - khong tinh phu phi lien vung. */
    private static final Set<String> NEARBY_PROVINCES = Set.of(
            "ha noi", "ho chi minh", "binh duong", "dong nai", "long an",
            "bac ninh", "hung yen", "hai duong");

    private final String baseUrl;

    public GhtkRestClient(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new GhtkApiException("Missing GHTK base url");
        }
        this.baseUrl = baseUrl;
    }

    /**
     * Gia lap GET {baseUrl}/services/shipment/fee?province=...&weight=...
     *
     * Tra ve CHUOI JSON TRO nhu mot API that, de Adapter phai tu boc tach.
     */
    public String getShipmentFee(String province, double weightKg) {
        if (province == null || province.isBlank()) {
            throw new GhtkApiException("province is required");
        }
        if (weightKg <= 0) {
            throw new GhtkApiException("weight must be greater than 0");
        }
        if (weightKg > MAX_KILOGRAMS) {
            // GHTK khong nhan kien qua nang - loi NGHIEP VU cua doi tac.
            throw new GhtkApiException(
                    "GHTK khong nhan kien hang tren " + (int) MAX_KILOGRAMS + "kg");
        }

        boolean nearby = NEARBY_PROVINCES.contains(province.toLowerCase(Locale.ROOT));
        int extraHalfKg = (int) Math.max(0, Math.ceil((weightKg - 1.0) / 0.5));
        int feeThousand = BASE_FEE_THOUSAND
                + extraHalfKg * FEE_PER_HALF_KG_THOUSAND
                + (nearby ? 0 : REMOTE_SURCHARGE_THOUSAND);
        int deliveryHours = nearby ? 36 : 72;

        return "{\"success\":true,\"fee\":{\"name\":\"GHTK Tiet Kiem\",\"fee\":" + feeThousand
                + ",\"unit\":\"nghin_dong\",\"delivery_time\":" + deliveryHours
                + ",\"include_vat\":true}}";
    }

    public String baseUrl() {
        return baseUrl;
    }
}
