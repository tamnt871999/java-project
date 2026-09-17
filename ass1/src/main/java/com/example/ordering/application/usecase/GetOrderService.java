package com.example.ordering.application.usecase;

import com.example.ordering.application.port.in.GetOrderUseCase;
import com.example.ordering.application.port.in.OrderNotFoundException;
import com.example.ordering.application.port.in.OrderView;
import com.example.ordering.application.port.out.OrderRepository;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderId;
import com.example.ordering.domain.OrderItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * INTERACTOR cho luong DOC.
 *
 * So voi PlaceOrderService, class nay ngan hon han - va do la dieu BINH THUONG.
 * Luong doc khong tao ra su kien nghiep vu nao, khong doi trang thai gi, nen
 * no khong co viec gi de dieu phoi ngoai ba buoc: tim, khong thay thi bao,
 * thay thi dich sang DTO bien.
 *
 * Mot cau hoi thuong gap: "mong the nay thi bo luon tang use case, cho
 * Controller goi thang Repository co duoc khong?"
 *
 * Duoc, va nhieu du an lam vay that - nguoi ta goi la CQRS: luong ghi di qua
 * day du cac vong, luong doc di duong tat. Doi lai ban mat hai thu:
 *   1. Controller bat dau biet den domain Order va tu quyet dinh 404.
 *   2. Khi nghiep vu doc phat sinh quy tac (an don da huy, che gia si voi
 *      khach le) thi khong con cho nao de dat no ngoai Controller.
 *
 * Bai nay giu day du vong de ban nhin thay hinh dang chuan truoc da.
 */
public class GetOrderService implements GetOrderUseCase {

    private final OrderRepository orderRepository;

    public GetOrderService(OrderRepository orderRepository) {
        this.orderRepository = Objects.requireNonNull(orderRepository, "orderRepository must not be null");
    }

    @Override
    public OrderView getOrder(String orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        // 1. Dich kieu nguyen thuy tu ben ngoai thanh Value Object cua domain.
        //    OrderId.of() tu chan chuoi rong / null ngay tai bien.
        OrderId id = OrderId.of(orderId);

        // 2. Hoi tang luu tru qua outbound port. Khong biet la SQL hay Mongo.
        Order order = orderRepository.findById(id)
                // 3. Bien "khong co" thanh loi nghiep vu. Day la QUYET DINH cua
                //    use case, khong phai cua repository.
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // 4. Tra DTO bien, khong tra aggregate ra ngoai.
        return toView(order);
    }

    private static OrderView toView(Order order) {
        List<OrderView.Line> lines = new ArrayList<>(order.items().size());
        for (OrderItem item : order.items()) {
            lines.add(new OrderView.Line(
                    item.productId().value(),
                    item.quantity().value(),
                    item.unitPrice().amount(),
                    item.lineTotal().amount()));
        }
        return new OrderView(
                order.id().value(),
                order.customerId().value(),
                order.status().name(),
                order.placedAt().toString(),
                lines,
                order.price().subtotal().amount(),
                order.price().discount().amount(),
                order.price().shippingFee().amount(),
                order.price().total().amount());
    }
}
