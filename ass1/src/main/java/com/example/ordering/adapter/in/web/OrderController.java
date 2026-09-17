package com.example.ordering.adapter.in.web;

import com.example.ordering.application.port.in.GetOrderUseCase;
import com.example.ordering.application.port.in.OrderNotFoundException;
import com.example.ordering.application.port.in.OrderView;
import com.example.ordering.application.port.in.PlaceOrderCommand;
import com.example.ordering.application.port.in.PlaceOrderResult;
import com.example.ordering.application.port.in.PlaceOrderUseCase;
import com.example.ordering.domain.DomainException;

import java.util.Objects;

/**
 * INBOUND ADAPTER - lifeline "OrderController (Inbound Adapter / Interface
 * Adapters)" trong sequence diagram.
 *
 * Chu y kieu cua truong: PlaceOrderUseCase, KHONG phai PlaceOrderService.
 * Controller chi biet INBOUND PORT, khong biet ai hien thuc no. Nho vay bo test
 * co the tiem mot ban gia vao ma khong can dung ca he thong.
 *
 * Ba viec duy nhat Controller duoc lam:
 *   1. Nhan du lieu tho tu ben ngoai (chuoi JSON).
 *   2. Dich thanh Command va goi use case qua port.
 *   3. Dich ket qua / exception thanh ngon ngu HTTP.
 *
 * BANG DICH LOI - trach nhiem quan trong nhat cua Controller:
 *   IllegalArgumentException (JSON hong, thieu truong) -> 400 Bad Request
 *   DomainException (vi pham quy tac nghiep vu)        -> 422 Unprocessable Entity
 *   OrderNotFoundException                             -> 404 Not Found
 *
 * Domain va use case khong he biet nhung con so nay ton tai.
 */
public class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    // Controller phu thuoc vao HAI inbound port rieng biet, khong phai mot
    // interface OrderService gop chung. Doi lay: co the tiem ban gia cho tung
    // use case, va them use case moi khong lam doi chu ky cua cai da co.
    public OrderController(PlaceOrderUseCase placeOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this.placeOrderUseCase = Objects.requireNonNull(placeOrderUseCase, "placeOrderUseCase must not be null");
        this.getOrderUseCase = Objects.requireNonNull(getOrderUseCase, "getOrderUseCase must not be null");
    }

    /** POST /orders */
    public ApiResponse placeOrder(String requestBody) {
        PlaceOrderCommand command;
        try {
            command = OrderJsonMapper.toCommand(Json.parse(requestBody));
        } catch (RuntimeException badRequest) {
            return ApiResponse.error(400, "BAD_REQUEST", badRequest.getMessage());
        }

        try {
            PlaceOrderResult result = placeOrderUseCase.placeOrder(command);
            return ApiResponse.created(OrderJsonMapper.toJson(result));
        } catch (DomainException violated) {
            return ApiResponse.error(422, "BUSINESS_RULE_VIOLATED", violated.getMessage());
        } catch (IllegalArgumentException invalid) {
            return ApiResponse.error(400, "BAD_REQUEST", invalid.getMessage());
        }
    }

    /**
     * GET /orders/{id}
     *
     * Ngan hon placeOrder vi khong co body de parse: tham so duy nhat da nam
     * san tren duong dan, do tang ha tang boc ra.
     *
     * Van la cung mot cong viec: dich ngon ngu HTTP thanh loi goi use case,
     * roi dich ket qua va exception nguoc lai thanh ma trang thai HTTP.
     */
    public ApiResponse getOrder(String orderId) {
        try {
            OrderView view = getOrderUseCase.getOrder(orderId);
            return ApiResponse.ok(OrderJsonMapper.toJson(view));
        } catch (OrderNotFoundException notFound) {
            // Use case nem exception nghiep vu, Controller doi thanh 404.
            // Day la DUY NHAT mot noi trong he thong biet so 404 ton tai.
            return ApiResponse.error(404, "ORDER_NOT_FOUND", notFound.getMessage());
        } catch (IllegalArgumentException invalid) {
            return ApiResponse.error(400, "BAD_REQUEST", invalid.getMessage());
        }
    }

}
