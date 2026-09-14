package com.example.ordering.application.port.out;

import com.example.ordering.domain.Order;

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
 * Port chi co DUNG MOT method, dung bang so message ma sequence diagram ve cho
 * lifeline nay. Khong them findById hay nextOrderId: moi method thua tren port
 * deu la mot canh cua ma tang ngoai co the thoc tay vao.
 *
 * Chu y chu ky ham: nhan va tra ve Order cua DOMAIN, tuyet doi khong nhac toi
 * OrderEntity, JPA hay SQL.
 */
public interface OrderRepository {

    /**
     * Luu don hang va tra ve chinh no kem MA DON vua duoc cap.
     *
     * Don hang di vao chua co ma don; ma don duoc tang luu tru sinh ra, dung
     * nhu @GeneratedValue cua JPA. Do la ly do ham nay phai tra ve Order chu
     * khong the tra ve void.
     */
    Order save(Order order);
}
