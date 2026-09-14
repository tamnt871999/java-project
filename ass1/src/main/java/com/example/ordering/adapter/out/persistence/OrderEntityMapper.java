package com.example.ordering.adapter.out.persistence;

import com.example.ordering.adapter.out.persistence.OrderEntity.OrderItemEntity;
import com.example.ordering.domain.CustomerId;
import com.example.ordering.domain.Money;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderId;
import com.example.ordering.domain.OrderItem;
import com.example.ordering.domain.OrderStatus;
import com.example.ordering.domain.PriceBreakdown;
import com.example.ordering.domain.ProductId;
import com.example.ordering.domain.Quantity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Dich qua lai giua ba the gioi: aggregate domain, entity luu tru, va dong du lieu.
 *
 * Ca ba deu duoc go gon trong file nay. Do la chu dich: neu schema database
 * doi ten cot, chi file nay do, khong file nao khac.
 */
final class OrderEntityMapper {

    // --- domain <-> entity -------------------------------------------------

    static OrderEntity toEntity(Order order) {
        List<OrderItemEntity> items = new ArrayList<>();
        for (OrderItem item : order.items()) {
            items.add(new OrderItemEntity(
                    order.id().value(),
                    item.productId().value(),
                    item.quantity().value(),
                    item.unitPrice().amount()));
        }
        return new OrderEntity(
                order.id().value(),
                order.customerId().value(),
                order.status().name(),
                order.price().subtotal().amount(),
                order.price().discount().amount(),
                order.price().shippingFee().amount(),
                order.price().total().amount(),
                order.placedAt().toString(),
                items);
    }

    static Order toDomain(OrderEntity entity) {
        List<OrderItem> items = new ArrayList<>();
        for (OrderItemEntity item : entity.items()) {
            items.add(new OrderItem(
                    ProductId.of(item.productId()),
                    Quantity.of(item.quantity()),
                    new Money(item.unitPrice())));
        }
        PriceBreakdown price = new PriceBreakdown(
                new Money(entity.subtotal()),
                new Money(entity.discount()),
                new Money(entity.shippingFee()),
                new Money(entity.total()));

        return Order.rehydrate(
                OrderId.of(entity.id()),
                CustomerId.of(entity.customerId()),
                items,
                price,
                OrderStatus.valueOf(entity.status()),
                Instant.parse(entity.placedAt()));
    }

    private OrderEntityMapper() {
    }
}
