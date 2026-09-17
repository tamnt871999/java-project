package com.example.ordering.adapter.out.persistence;

import com.example.ordering.adapter.out.persistence.OrderEntity.OrderItemEntity;
import com.example.ordering.infrastructure.jpa.EntityMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SO DO ANH XA entity xuong bang - tuong duong phan annotation cua JPA.
 *
 * Trong Spring Boot, thong tin trong file nay duoc khai bao ngay tren
 * OrderEntity bang @Table(name = "orders"), @Column(name = "customer_id"),
 * @OneToMany(mappedBy = "order")... roi Hibernate doc bang reflection.
 *
 * O day ta viet tuong minh de nhin thay ro rang framework dang lam gi:
 *
 *   orders(id, customer_id, status, subtotal, discount, shipping_fee, total, placed_at)
 *   order_items(order_id, product_id, quantity, unit_price)
 */
public final class OrderPersistenceMapping {

    public static final EntityMapping<OrderEntity, String> ORDER_MAPPING = new EntityMapping<>(
            "orders",
            "order_items",
            "order_id",
            OrderEntity::id,
            OrderPersistenceMapping::toRow,
            OrderPersistenceMapping::toChildRows,
            OrderPersistenceMapping::fromRows);

    private static Map<String, Object> toRow(OrderEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.id());
        row.put("customer_id", entity.customerId());
        row.put("status", entity.status());
        row.put("subtotal", entity.subtotal());
        row.put("discount", entity.discount());
        row.put("shipping_fee", entity.shippingFee());
        row.put("total", entity.total());
        row.put("placed_at", entity.placedAt());
        return row;
    }

    private static List<Map<String, Object>> toChildRows(OrderEntity entity) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (OrderItemEntity item : entity.items()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("order_id", item.orderId());
            row.put("product_id", item.productId());
            row.put("quantity", item.quantity());
            row.put("unit_price", item.unitPrice());
            rows.add(row);
        }
        return rows;
    }


    /**
     * CHIEU NGUOC: cac dong du lieu tho -> OrderEntity.
     *
     * Trong Spring Boot day la viec Hibernate lam am tham sau moi cau SELECT,
     * dung chinh bo annotation da dung de ghi. Viet tay ra moi thay no chi la
     * mot phep dich thuan tuy, khong co phep mau nao ca.
     */
    private static OrderEntity fromRows(Map<String, Object> row, List<Map<String, Object>> childRows) {
        List<OrderItemEntity> items = new ArrayList<>();
        for (Map<String, Object> child : childRows) {
            items.add(new OrderItemEntity(
                    text(child, "order_id"),
                    text(child, "product_id"),
                    (Integer) child.get("quantity"),
                    (BigDecimal) child.get("unit_price")));
        }
        return new OrderEntity(
                text(row, "id"),
                text(row, "customer_id"),
                text(row, "status"),
                (BigDecimal) row.get("subtotal"),
                (BigDecimal) row.get("discount"),
                (BigDecimal) row.get("shipping_fee"),
                (BigDecimal) row.get("total"),
                text(row, "placed_at"),
                items);
    }

    private static String text(Map<String, Object> row, String column) {
        return String.valueOf(row.get(column));
    }

    private OrderPersistenceMapping() {
    }
}
