package com.example.ordering.application.port.in;

/**
 * INBOUND PORT cho luong DOC.
 *
 * Tach khoi PlaceOrderUseCase theo tinh than CQRS: doc va ghi co nhu cau rat
 * khac nhau (doc khong doi trang thai, khong can transaction).
 *
 * Co mat port nay de header "Location: /orders/{id}" tra ve o buoc 201 la mot
 * dia chi that su goi duoc, khong phai loi hua suong.
 */
public interface FindOrderUseCase {

    PlaceOrderResult findById(String orderId);
}
