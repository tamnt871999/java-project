package com.example.ordering.domain;

import java.util.List;

/**
 * DOMAIN SERVICE - lifeline "OrderPricingService (Domain Service / Entities)"
 * trong sequence diagram.
 *
 * Vi sao khong nhet thang vao Order? Vi chinh sach gia thay doi theo chien dich
 * kinh doanh, con ban chat mot don hang thi khong. Tach ra service rieng cho
 * phep doi chinh sach ma khong sua aggregate.
 *
 * Class nay la logic nghiep vu THUAN TUY: khong DB, khong mang, khong thoi gian
 * he thong. Nho vay no kiem thu duoc bang phep tinh tay.
 *
 * Quy tac dang ap dung:
 *   - Giam 10% khi tam tinh tu 500 tro len.
 *   - Mien phi ship khi tam tinh tu 100 tro len, nguoc lai thu 9.99.
 */
public class OrderPricingService {

    private static final Money DISCOUNT_THRESHOLD = Money.of("500.00");
    private static final int DISCOUNT_PERCENT = 10;
    private static final Money FREE_SHIPPING_THRESHOLD = Money.of("100.00");
    private static final Money SHIPPING_FEE = Money.of("9.99");

    /** Buoc calculateTotal(items) trong sequence diagram. */
    public PriceBreakdown calculateTotal(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new DomainException("Khong the tinh gia cho don hang rong");
        }

        Money subtotal = items.stream()
                .map(OrderItem::lineTotal)
                .reduce(Money.ZERO, Money::plus);

        Money discount = subtotal.isAtLeast(DISCOUNT_THRESHOLD)
                ? subtotal.percent(DISCOUNT_PERCENT)
                : Money.ZERO;

        Money shippingFee = subtotal.isAtLeast(FREE_SHIPPING_THRESHOLD)
                ? Money.ZERO
                : SHIPPING_FEE;

        Money total = subtotal.minus(discount).plus(shippingFee);
        return new PriceBreakdown(subtotal, discount, shippingFee, total);
    }
}
