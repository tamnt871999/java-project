package com.example.ordering.application.usecase;

import com.example.ordering.application.port.in.PlaceOrderCommand;
import com.example.ordering.application.port.in.PlaceOrderResult;
import com.example.ordering.application.port.in.PlaceOrderUseCase;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.CustomerId;
import com.example.ordering.domain.Money;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderItem;
import com.example.ordering.domain.OrderPricingService;
import com.example.ordering.domain.PriceBreakdown;
import com.example.ordering.domain.ProductId;
import com.example.ordering.domain.Quantity;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * INTERACTOR - lifeline "PlaceOrderService (Interactor / Use Case)".
 *
 * Day la nguoi DIEU PHOI, khong phai nguoi ra quyet dinh nghiep vu:
 *   - Gia bao nhieu  -> hoi OrderPricingService (domain service).
 *   - Don co hop le  -> hoi Order.place() (aggregate).
 *   - Luu o dau      -> goi qua OrderRepository (outbound port), khong biet la JPA hay file.
 *
 * Toan bo phu thuoc deu la INTERFACE hoac class thuan cua domain. Khong mot
 * dong nao trong file nay biet toi HTTP, JSON, SQL hay Spring.
 *
 * Trinh tu duoi day khop dung voi sequence diagram:
 *   placeOrder(command) -> calculateTotal(items) -> total -> save(order) -> Result
 */
public class PlaceOrderService implements PlaceOrderUseCase {

    private final OrderPricingService pricingService;
    private final OrderRepository orderRepository;
    private final Clock clock;

    public PlaceOrderService(OrderPricingService pricingService,
                             OrderRepository orderRepository,
                             Clock clock) {
        this.pricingService = Objects.requireNonNull(pricingService, "pricingService must not be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
        // Thoi gian la mot phu thuoc, khong phai hang so toan cuc: tiem Clock vao
        // de bo test co the dong bang thoi gian thay vi phu thuoc Instant.now().
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public PlaceOrderResult placeOrder(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        // 1. Dich DTO bien thanh Value Object cua domain.
        //    Moi gia tri sai (so luong 0, gia am) bi chan ngay tai day boi chinh VO.
        List<OrderItem> items = toOrderItems(command);

        // 2. calculateTotal(items) -> total   [domain service tinh gia]
        PriceBreakdown price = pricingService.calculateTotal(items);

        // 3. Aggregate tu kiem tra invariant roi tao don hang.
        //    Chua co ma don o buoc nay - ma don do tang luu tru cap.
        Order order = Order.place(
                CustomerId.of(command.customerId()), items, price, Instant.now(clock));

        // 4. save(order)  [qua outbound port - khong biet ai hien thuc]
        //    Don tra ve da mang ma don, giong khi JPA tra entity sau @GeneratedValue.
        Order savedOrder = orderRepository.save(order);

        // 5. Tra ve DTO bien, khong tra aggregate.
        return toResult(savedOrder);
    }

    private List<OrderItem> toOrderItems(PlaceOrderCommand command) {
        List<OrderItem> items = new ArrayList<>(command.items().size());
        for (PlaceOrderCommand.Item item : command.items()) {
            items.add(new OrderItem(
                    ProductId.of(item.productId()),
                    Quantity.of(item.quantity()),
                    Money.of(item.unitPrice())));
        }
        return items;
    }

    private static PlaceOrderResult toResult(Order order) {
        return new PlaceOrderResult(order.id().value(), order.price().total().amount());
    }
}
