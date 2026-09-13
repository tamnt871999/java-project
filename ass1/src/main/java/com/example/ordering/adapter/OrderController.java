package com.example.ordering.adapter;

import com.example.ordering.application.OrderService;
import com.example.ordering.application.PlaceOrderRequest;
import com.example.ordering.application.ServiceException;
import com.example.ordering.domain.DomainException;
import com.example.ordering.domain.Order;
import com.example.ordering.domain.OrderStatus;

import java.util.Objects;

/**
 * REST CONTROLLER cho tai nguyen /api/orders.
 *
 * BANG DICH LOI - day la trach nhiem quan trong nhat cua Controller trong REST:
 *
 *   ServiceException NOT_FOUND        -> 404 Not Found
 *   ServiceException INVALID          -> 400 Bad Request
 *   ServiceException OUT_OF_STOCK     -> 409 Conflict
 *   ServiceException PAYMENT_DECLINED -> 402 Payment Required
 *   DomainException                   -> 409 Conflict (sai trang thai)
 *   JSON hong / thieu truong          -> 400 Bad Request
 *
 * Tang domain va application khong he biet cac con so nay ton tai.
 */
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = Objects.requireNonNull(orderService);
    }

    /** POST /api/orders */
    public ApiResponse create(String requestBody) {
        PlaceOrderRequest request;
        try {
            request = OrderJson.toRequest(Json.parse(requestBody));
        } catch (RuntimeException badJson) {
            return ApiResponse.error(400, "BAD_REQUEST", badJson.getMessage());
        }

        try {
            Order order = orderService.placeOrder(request);
            return ApiResponse.created(OrderJson.toJson(order), "/api/orders/" + order.getId());
        } catch (ServiceException | DomainException failure) {
            return toErrorResponse(failure);
        }
    }

    /** GET /api/orders?status=PAID */
    public ApiResponse list(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()) {
            return ApiResponse.ok(OrderJson.toJson(orderService.findAll()));
        }
        try {
            OrderStatus status = OrderStatus.valueOf(statusFilter.toUpperCase());
            return ApiResponse.ok(OrderJson.toJson(orderService.findByStatus(status)));
        } catch (IllegalArgumentException unknownStatus) {
            return ApiResponse.error(400, "BAD_REQUEST",
                    "Trang thai khong hop le: " + statusFilter + ". Cho phep: NEW, PAID, CANCELLED");
        }
    }

    /** GET /api/orders/{id} */
    public ApiResponse getOne(String orderId) {
        try {
            return ApiResponse.ok(OrderJson.toJson(orderService.findById(orderId)));
        } catch (ServiceException failure) {
            return toErrorResponse(failure);
        }
    }

    /** POST /api/orders/{id}/pay */
    public ApiResponse pay(String orderId) {
        try {
            return ApiResponse.ok(OrderJson.toJson(orderService.pay(orderId)));
        } catch (ServiceException | DomainException failure) {
            return toErrorResponse(failure);
        }
    }

    /** POST /api/orders/{id}/cancel */
    public ApiResponse cancel(String orderId) {
        try {
            return ApiResponse.ok(OrderJson.toJson(orderService.cancel(orderId, "Khach huy don")));
        } catch (ServiceException | DomainException failure) {
            return toErrorResponse(failure);
        }
    }

    /** Dich exception cua tang duoi thanh ma trang thai HTTP. */
    private ApiResponse toErrorResponse(RuntimeException failure) {
        if (failure instanceof ServiceException serviceFailure) {
            int status = switch (serviceFailure.getCode()) {
                case "NOT_FOUND" -> 404;
                case "OUT_OF_STOCK" -> 409;
                case "PAYMENT_DECLINED" -> 402;
                default -> 400;
            };
            return ApiResponse.error(status, serviceFailure.getCode(),
                    serviceFailure.getMessage(), serviceFailure.getDetails());
        }
        // DomainException: du lieu dung cu phap nhung vi pham trang thai cua Model.
        return ApiResponse.error(409, "CONFLICT", failure.getMessage());
    }
}
