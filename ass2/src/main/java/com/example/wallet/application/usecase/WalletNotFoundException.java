package com.example.wallet.application.usecase;

import java.util.UUID;

/**
 * Khong tim thay vi voi dinh danh da cho.
 *
 * Vi sao KHONG dung DomainException cho truong hop nay?
 *
 * DomainException co nghia "mot luat bat bien cua nghiep vu bi vi pham". Con
 * "khong tim thay vi" khong vi pham luat nao ca - no la chuyen cua tang ung
 * dung: ben goi dua vao mot dinh danh khong ton tai. Gop hai loai lai thi tang
 * ngoai mat kha nang phan biet "du lieu dau vao sai" (thuong la 404) voi "thao
 * tac bi nghiep vu tu choi" (thuong la 409 / 422).
 */
public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID walletId) {
        super("Khong tim thay vi dien tu: " + walletId);
    }
}
