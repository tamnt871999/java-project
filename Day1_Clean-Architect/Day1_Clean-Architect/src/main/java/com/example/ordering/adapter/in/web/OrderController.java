package com.example.ordering.adapter.in.web;

import com.example.ordering.application.port.in.GetOrderUseCase;
import com.example.ordering.application.port.in.GetOrderUseCase.OrderView;
import com.example.ordering.application.port.in.PlaceOrderUseCase;
import com.example.ordering.application.port.in.PlaceOrderUseCase.PlaceOrderCommand;
import com.example.ordering.application.port.in.PlaceOrderUseCase.PlaceOrderResult;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
class OrderController {

    private final PlaceOrderUseCase placeOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    OrderController(PlaceOrderUseCase placeOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this.placeOrderUseCase = placeOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlaceOrderResult placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        List<PlaceOrderCommand.Item> items = request.items().stream()
                .map(item -> new PlaceOrderCommand.Item(
                        item.productId(), item.quantity(), item.unitPrice()))
                .toList();

        return placeOrderUseCase.placeOrder(new PlaceOrderCommand(request.customerId(), items));
    }

    @GetMapping("/{orderId}")
    OrderView getOrder(@PathVariable("orderId") Long orderId) {
        return getOrderUseCase.getOrder(orderId);
    }

    record PlaceOrderRequest(

            @NotBlank(message = "Thieu truong bat buoc: customerId")
            String customerId,

            @NotEmpty(message = "Truong items phai la mang khong rong")
            @Valid
            List<Item> items) {

        record Item(

                @NotBlank(message = "Thieu truong bat buoc: productId")
                String productId,

                @NotNull(message = "Thieu truong bat buoc: quantity")
                Integer quantity,

                @NotNull(message = "Thieu truong bat buoc: unitPrice")
                BigDecimal unitPrice) {
        }
    }
}
