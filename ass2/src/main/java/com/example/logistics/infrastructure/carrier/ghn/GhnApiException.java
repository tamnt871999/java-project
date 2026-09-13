package com.example.logistics.infrastructure.carrier.ghn;

/**
 * Ngoai le RIENG cua SDK GHN.
 *
 * Kieu nay tuyet doi khong duoc phep xuat hien trong tang Core - GhnCarrierAdapter
 * co trach nhiem bat no va dich sang CarrierUnavailableException.
 */
public class GhnApiException extends RuntimeException {

    public GhnApiException(String message) {
        super(message);
    }
}
