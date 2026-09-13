package com.example.logistics.core.port.in;

import com.example.logistics.core.domain.ShipmentRequest;
import com.example.logistics.core.domain.ShippingQuote;

/**
 * INBOUND PORT (driving port) - cua vao cua loi.
 *
 * Tang ngoai (CLI, REST controller, message consumer) chi duoc phep nhin thay
 * interface nay, khong duoc cam truc tiep class CalculateShippingFeeUseCase.
 * Nho the co the thay ban hien thuc (them cache, them log, them retry bang
 * Decorator) ma khong dong vao mot dong nao cua tang ngoai.
 */
public interface CalculateShippingFeePort {

    ShippingQuote calculate(ShipmentRequest request);
}
