package com.example.ordering.application.port.in;

/**
 * INBOUND PORT cho luong DOC.
 *
 * Vi sao khong them method getOrder() vao thang PlaceOrderUseCase cho gon?
 *
 * Vi Interface Segregation Principle: moi adapter chi nen phu thuoc vao dung
 * phan no can. Mot man hinh chi hien thi don hang khong co ly do gi phai cam
 * trong tay cai quyen tao don. Tach ra con giup bo test gia lap tung use case
 * rieng, va sau nay muon tach doc/ghi ra hai service khac nhau thi khong phai
 * sua hop dong.
 *
 * Quy uoc trong Clean Architecture: MOT use case = MOT interface = MOT method.
 */
public interface GetOrderUseCase {

    /**
     * @throws OrderNotFoundException neu khong co don hang voi ma nay
     */
    OrderView getOrder(String orderId);
}
