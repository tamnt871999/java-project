package com.example.ordering.application;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loi nghiep vu o tang Service.
 *
 * Mang theo 3 thu:
 *   - code    : de Controller dich sang HTTP status ma khong phai doan tu message.
 *   - message : cau mo ta cho con nguoi doc.
 *   - details : SO LIEU dang thuan so, de client tu quyet dinh cach hien thi.
 *
 * Vi sao can details? Vi neu nhet "$1,000.00" vao message thi tang Service da
 * am tham quyet dinh dinh dang hien thi - viec do phu thuoc ngon ngu va vung
 * mien cua nguoi dung, khong phai viec cua nghiep vu.
 */
public class ServiceException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    private ServiceException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public static ServiceException notFound(String message) {
        return new ServiceException("NOT_FOUND", message, Map.of());
    }

    public static ServiceException invalid(String message) {
        return new ServiceException("INVALID", message, Map.of());
    }

    public static ServiceException outOfStock(String productId, int requested, int available) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("productId", productId);
        details.put("requested", requested);
        details.put("available", available);
        return new ServiceException("OUT_OF_STOCK",
                "Khong du hang cho san pham " + productId, details);
    }

    public static ServiceException paymentDeclined(java.math.BigDecimal total,
                                                   java.math.BigDecimal limit) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("total", total);
        details.put("limit", limit);
        return new ServiceException("PAYMENT_DECLINED",
                "Cong thanh toan tu choi don hang vi vuot han muc", details);
    }

    public String getCode() {
        return code;
    }

    /** Map bat bien, chi chua kieu nguyen thuy va BigDecimal - khong dinh dang gi. */
    public Map<String, Object> getDetails() {
        return details;
    }
}
