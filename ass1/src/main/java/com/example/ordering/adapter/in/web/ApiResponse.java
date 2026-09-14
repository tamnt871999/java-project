package com.example.ordering.adapter.in.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ket qua Controller tra ve: ma trang thai + body se duoc serialize thanh JSON.
 *
 * Nho lop nay, Controller quyet dinh duoc 200 / 201 / 404 ma khong phai dong
 * cham vao API cua HTTP server - viec ghi byte ra socket la cua tang
 * infrastructure.
 */
public record ApiResponse(int status, Object body, Map<String, String> headers) {

    /** 201 Created - buoc cuoi cung trong sequence diagram. */
    public static ApiResponse created(Object body) {
        return new ApiResponse(201, body, Map.of());
    }

    public static ApiResponse error(int status, String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
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
