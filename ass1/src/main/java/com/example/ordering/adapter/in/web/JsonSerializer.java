package com.example.ordering.adapter.in.web;

/**
 * Cong serialize cua vong Interface Adapters.
 *
 * Vi sao can lop mong nay? Vi viec bien du lieu thanh JSON la trach nhiem cua
 * INTERFACE ADAPTERS, con tang infrastructure chi duoc lam mot viec: day byte
 * ra socket. Neu de HttpServerRunner tu goi bo JSON thi ha tang da lam thay
 * phan viec cua adapter.
 */
public final class JsonSerializer {

    public static String toJson(Object body) {
        return Json.write(body);
    }

    private JsonSerializer() {
    }
}
