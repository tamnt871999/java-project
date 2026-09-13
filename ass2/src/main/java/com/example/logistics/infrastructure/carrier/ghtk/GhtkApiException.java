package com.example.logistics.infrastructure.carrier.ghtk;

/** Ngoai le rieng cua ha tang GHTK - khong duoc phep lot vao tang Core. */
public class GhtkApiException extends RuntimeException {

    public GhtkApiException(String message) {
        super(message);
    }
}
