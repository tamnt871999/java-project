package com.example.ordering.application.port.out;

import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderId;

import java.util.Optional;

/**
 * OUTBOUND PORT - cong ma use case dung de noi ra the gioi ben ngoai.
 *
 * DAY LA DIEM MAU CHOT CUA CLEAN ARCHITECTURE:
 *
 * Interface nay do tang APPLICATION so huu, con class hien thuc no
 * (JpaOrderRepositoryAdapter) nam o tang ADAPTER ben ngoai. Nho vay luc CHAY
 * thi use case goi ra ngoai, nhung luc BIEN DICH thi mui ten phu thuoc van chi
 * VAO TRONG. Do la Dependency Inversion.
 *
 * So sanh voi Spring quen thuoc:
 *
 *   MVC   : interface OrderRepository extends JpaRepository<OrderEntity, String>
 *           -> interface thuoc ve tang persistence, service phai import xuong.
 *   Clean : interface nay thuoc ve tang application, persistence phai import len.
 *
 * Cung mot loi goi luc chay, nguoc chieu nhau luc bien dich.
 *
 * Chu y chu ky ham: nhan va tra ve Order cua DOMAIN, tuyet doi khong nhac toi
 * OrderEntity, JPA hay SQL. Port chi mo dung nhung cua ma use case thuc su
 * dung - moi method thua deu la mot canh cua ma tang ngoai co the thoc tay vao.
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

    /**
     * Doc lai don hang theo ma.
     *
     * Tra Optional chu khong tra null, va cung khong nem exception: "khong co"
     * la mot ket qua BINH THUONG cua viec tim kiem, khong phai su co. Viec coi
     * no la loi hay khong la quyet dinh cua use case - GetOrderService chon
     * cach nem OrderNotFoundException, mot use case khac co the chon tao don
     * moi. Port khong ap dat.
     */
    Optional<Order> findById(OrderId orderId);
}
