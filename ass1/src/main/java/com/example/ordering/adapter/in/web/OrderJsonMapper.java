package com.example.ordering.adapter.in.web;

import com.example.ordering.application.port.in.OrderView;
import com.example.ordering.application.port.in.PlaceOrderCommand;
import com.example.ordering.application.port.in.PlaceOrderResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dich giua the gioi JSON va DTO bien cua use case.
 *
 * Day la lop chong soc: doi ten truong tren API, ra ban v2, doi tu JSON sang
 * XML - tat ca chi cham vao file nay. Use case va domain khong hay biet.
 */
final class OrderJsonMapper {

    /** JSON -> PlaceOrderCommand. Loi cu phap nem IllegalArgumentException -> 400. */
    @SuppressWarnings("unchecked")
    static PlaceOrderCommand toCommand(Object json) {
        if (!(json instanceof Map)) {
            throw new IllegalArgumentException("Body phai la mot JSON object");
        }
        Map<String, Object> body = (Map<String, Object>) json;

        Object rawItems = body.get("items");
        if (!(rawItems instanceof List) || ((List<?>) rawItems).isEmpty()) {
            throw new IllegalArgumentException("Truong items phai la mang khong rong");
        }

        List<PlaceOrderCommand.Item> items = new ArrayList<>();
        for (Object rawItem : (List<Object>) rawItems) {
            if (!(rawItem instanceof Map)) {
                throw new IllegalArgumentException("Moi phan tu cua items phai la JSON object");
            }
            Map<String, Object> item = (Map<String, Object>) rawItem;
            items.add(new PlaceOrderCommand.Item(
                    text(item, "productId"),
                    number(item, "quantity").intValueExact(),
                    number(item, "unitPrice").toPlainString()));
        }
        return new PlaceOrderCommand(text(body, "customerId"), items);
    }

    /** PlaceOrderResult -> JSON. Dung hai truong nhu buoc 201 Created trong hinh. */
    static Map<String, Object> toJson(PlaceOrderResult result) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("orderId", result.orderId());
        json.put("total", result.total());
        return json;
    }

    /**
     * OrderView -> JSON cho GET /orders/{id}.
     *
     * De y: ten truong tren JSON do CHINH FILE NAY quyet dinh. Muon doi
     * "shippingFee" thanh "shipping_fee" cho hop chuan cua doi frontend thi
     * sua o day, va khong mot dong nao trong application hay domain phai doi.
     * Do la gia tri thuc te cua lop mapper ma nhieu nguoi cho la thua.
     */
    static Map<String, Object> toJson(OrderView view) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderView.Line line : view.items()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", line.productId());
            item.put("quantity", line.quantity());
            item.put("unitPrice", line.unitPrice());
            item.put("lineTotal", line.lineTotal());
            items.add(item);
        }

        Map<String, Object> price = new LinkedHashMap<>();
        price.put("subtotal", view.subtotal());
        price.put("discount", view.discount());
        price.put("shippingFee", view.shippingFee());
        price.put("total", view.total());

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("orderId", view.orderId());
        json.put("customerId", view.customerId());
        json.put("status", view.status());
        json.put("placedAt", view.placedAt());
        json.put("items", items);
        json.put("price", price);
        return json;
    }

    private static String text(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Thieu truong bat buoc: " + field);
        }
        return s;
    }

    private static BigDecimal number(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (!(value instanceof BigDecimal number)) {
            throw new IllegalArgumentException("Truong " + field + " phai la so");
        }
        return number;
    }

    private OrderJsonMapper() {
    }
}
