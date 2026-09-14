package com.example.ordering.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;

/**
 * MODEL LUU TRU - co tinh dat TACH RIENG khoi aggregate Order cua domain.
 *
 * Trong du an Spring that, class nay mang annotation:
 *
 *     @Entity @Table(name = "orders")
 *     public class OrderEntity {
 *         @Id private String id;
 *         @OneToMany(cascade = ALL) private List<OrderItemEntity> items;
 *         ...
 *     }
 *
 * Vi sao khong dung thang Order lam @Entity cho nhanh? Vi khi do domain se bi
 * keo theo constructor rong, setter, proxy lazy-loading va ca vong doi cua
 * EntityManager. Chi can vai thang la "kien truc" chi con la ten goi.
 *
 * Gia phai tra la phai viet mapper. Doi lai, domain duoc tu do.
 *
 * Ghi chu: JPA that yeu cau constructor khong tham so nen phai dung class
 * thuong; o day dung record cho gon vi ta tu viet lop luu tru.
 */
public record OrderEntity(String id,
                          String customerId,
                          String status,
                          BigDecimal subtotal,
                          BigDecimal discount,
                          BigDecimal shippingFee,
                          BigDecimal total,
                          String placedAt,
                          List<OrderItemEntity> items) {

    /** Dong trong bang order_items, tro ve don hang cha bang khoa ngoai order_id. */
    public record OrderItemEntity(String orderId,
                                  String productId,
                                  int quantity,
                                  BigDecimal unitPrice) {
    }
}
