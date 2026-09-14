package com.example.ordering.adapter.in.web;

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

    public OrderController(PlaceOrderUseCase placeOrderUseCase) {
        this.placeOrderUseCase = Objects.requireNonNull(placeOrderUseCase, "placeOrderUseCase must not be null");
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

}
