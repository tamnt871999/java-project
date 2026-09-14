package com.example.ordering.application.port.out;

import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderId;

import java.util.Optional;

/**
 * OUTBOUND PORT - lifeline "OrderRepository (Outbound Port / Application)".
 *
 * DAY LA DIEM MAU CHOT CUA CLEAN ARCHITECTURE:
 *
 * Interface nay do tang APPLICATION so huu, con class hien thuc no
 * (JpaOrderRepositoryAdapter) nam o tang ADAPTER ben ngoai. Nho vay luc CHAY
 * thi use case goi ra ngoai, nhung luc BIEN DICH thi mui ten phu thuoc van chi
 * VAO TRONG. Do la Dependency Inversion.
 *
 * Hay chu y chu ky ham: no nhan va tra ve Order cua DOMAIN, tuyet doi khong
 * nhac toi OrderEntity, JPA hay SQL. Neu mot ngay interface nay xuat hien tu
 * "Entity" hay "Jpa", nghia la ha tang da ro ri vao trong loi.
 */
public interface OrderRepository {

    /** Luu don hang. Tra ve chinh aggregate da duoc luu. */
    Order save(Order order);

    Optional<Order> findById(OrderId orderId);

    /** Sinh dinh danh moi cho don hang. */
    OrderId nextOrderId();
}
