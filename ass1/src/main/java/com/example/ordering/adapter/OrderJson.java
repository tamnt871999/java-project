package com.example.ordering.adapter;

import com.example.ordering.application.PlaceOrderRequest;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderLine;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REPRESENTATION cua Order - thay the cho View HTML o ban MVC truoc.
 *
 * Trong REST, "representation" la hinh hai ma tai nguyen duoc phoi bay ra
 * ngoai. Doi hinh hai nay (them truong, doi ten truong, ra API v2) chi anh
 * huong dung file nay, khong lan vao domain hay service.
 */
final class OrderJson {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Domain -> JSON. */
    static Map<String, Object> toJson(Order order) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("orderId", order.getId());
        json.put("customerId", order.getCustomerId());
        json.put("customerName", order.getCustomerName());
        json.put("shippingAddress", order.getShippingAddress());
        json.put("status", order.getStatus().name());
        json.put("statusLabel", order.getStatus().label());
        json.put("createdAt", order.getCreatedAt().format(TIME));
        json.put("subtotal", order.getSubtotal().amount());
        json.put("discount", order.getDiscount().amount());
        json.put("shippingFee", order.getShippingFee().amount());
        json.put("total", order.getTotal().amount());
        json.put("totalItems", order.getTotalItems());
        json.put("note", order.getNote());

        List<Object> lines = new ArrayList<>();
        for (OrderLine line : order.getLines()) {
            Map<String, Object> lineJson = new LinkedHashMap<>();
            lineJson.put("productId", line.getProductId());
            lineJson.put("productName", line.getProductName());
            lineJson.put("unitPrice", line.getUnitPrice().amount());
            lineJson.put("quantity", line.getQuantity());
            lineJson.put("lineTotal", line.getLineTotal().amount());
            lines.add(lineJson);
        }
        json.put("lines", lines);
        return json;
    }

    static List<Object> toJson(List<Order> orders) {
        List<Object> result = new ArrayList<>();
        orders.forEach(order -> result.add(toJson(order)));
        return result;
    }

    /**
     * JSON -> doi tuong dau vao cua Service.
     *
     * Moi loi cu phap deu duoc nem duoi dang IllegalArgumentException de
     * Controller dich thanh HTTP 400.
     */
    @SuppressWarnings("unchecked")
    static PlaceOrderRequest toRequest(Object json) {
        if (!(json instanceof Map)) {
            throw new IllegalArgumentException("Body phai la mot JSON object");
        }
        Map<String, Object> body = (Map<String, Object>) json;

        PlaceOrderRequest request = new PlaceOrderRequest(
                text(body, "customerId"), text(body, "shippingAddress"));

        Object rawItems = body.get("items");
        if (!(rawItems instanceof List) || ((List<?>) rawItems).isEmpty()) {
            throw new IllegalArgumentException("Truong items phai la mang khong rong");
        }
        for (Object rawItem : (List<Object>) rawItems) {
            if (!(rawItem instanceof Map)) {
                throw new IllegalArgumentException("Moi phan tu cua items phai la JSON object");
            }
            Map<String, Object> item = (Map<String, Object>) rawItem;
            Object quantity = item.get("quantity");
            if (!(quantity instanceof BigDecimal)) {
                throw new IllegalArgumentException("Truong quantity phai la so");
            }
            request.addItem(text(item, "productId"), ((BigDecimal) quantity).intValueExact());
        }
        return request;
    }

    private static String text(Map<String, Object> source, String field) {
        Object value = source.get(field);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Thieu truong bat buoc: " + field);
        }
        return s;
    }

    private OrderJson() {
    }
}
