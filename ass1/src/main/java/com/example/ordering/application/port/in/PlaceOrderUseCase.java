package com.example.ordering.application.port.in;

/**
 * INBOUND PORT - lifeline "PlaceOrderUseCase (Inbound Port / Application)".
 *
 * Hop dong tra loi cau hoi "ung dung nay lam duoc gi", viet duoi goc nhin
 * nghiep vu chu khong phai goc nhin ky thuat.
 *
 * Moi driving adapter (REST controller, CLI, message consumer, bo test) deu
 * goi qua interface nay va CHI qua no. Doi adapter khong lam thay doi mot dong
 * nao ben trong use case.
 */
public interface PlaceOrderUseCase {

    PlaceOrderResult placeOrder(PlaceOrderCommand command);
}
