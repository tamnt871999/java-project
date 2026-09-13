package com.example.logistics.core.port.out;

import com.example.logistics.core.domain.CarrierCode;

/**
 * Su co phia nha van chuyen, da duoc DICH sang ngon ngu cua loi.
 *
 * Adapter bat GhnApiException / IOException / timeout ... roi nem lai bang
 * kieu nay. Nho vay Use Case xu ly su co ma khong can import bat ky kieu
 * ngoai le nao cua SDK doi tac - neu khong, chi tiet ha tang lai ro ri vao
 * loi qua duong "throws".
 */
public class CarrierUnavailableException extends RuntimeException {

    private final CarrierCode carrier;

    public CarrierUnavailableException(CarrierCode carrier, String message) {
        super(message);
        this.carrier = carrier;
    }

    public CarrierUnavailableException(CarrierCode carrier, String message, Throwable cause) {
        super(message, cause);
        this.carrier = carrier;
    }

    public CarrierCode getCarrier() {
        return carrier;
    }
}
