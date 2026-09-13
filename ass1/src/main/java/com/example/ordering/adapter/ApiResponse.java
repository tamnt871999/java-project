package com.example.ordering.adapter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ket qua ma mot Controller tra ve: HTTP status + body se duoc serialize sang JSON.
 *
 * Nho lop nay, Controller quyet dinh duoc ma trang thai (200 / 201 / 404 ...)
 * ma khong phai dong cham vao API cua HTTP server.
 */
public record ApiResponse(int status, Object body, Map<String, String> headers) {

    public static ApiResponse ok(Object body) {
        return new ApiResponse(200, body, Map.of());
    }

    /** 201 kem header Location tro toi tai nguyen vua tao - dung chuan REST. */
    public static ApiResponse created(Object body, String location) {
        return new ApiResponse(201, body, Map.of("Location", location));
    }

    public static ApiResponse error(int status, String code, String message) {
        return error(status, code, message, Map.of());
    }

    /**
     * Loi kem so lieu may doc duoc, vi du:
     *   {"code":"OUT_OF_STOCK","message":"...","details":{"requested":9,"available":2}}
     * Client tu quyet dinh hien thi so do the nao.
     */
    public static ApiResponse error(int status, String code, String message,
                                    Map<String, Object> details) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (!details.isEmpty()) {
            body.put("details", details);
        }
        return new ApiResponse(status, body, Map.of());
    }

    /** 405 bat buoc phai kem header Allow liet ke cac method duoc phep. */
    public static ApiResponse methodNotAllowed(String allowed) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "METHOD_NOT_ALLOWED");
        body.put("message", "Method khong duoc ho tro. Cho phep: " + allowed);
        return new ApiResponse(405, body, Map.of("Allow", allowed));
    }
}
