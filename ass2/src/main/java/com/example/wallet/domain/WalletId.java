package com.example.wallet.domain;

import java.util.UUID;
import java.util.Objects;

/**
 * VALUE OBJECT dinh danh vi dien tu.
 *
 * Ma nguon cu de "public UUID id". Boc lai thanh kieu rieng de dinh danh cua
 * aggregate noi bang ngon ngu cua domain: outbound port viet
 * findById(WalletId) chu khong phai findById(UUID) - doc ra la biet dang tim
 * vi nao, khong phai tra cuu xem UUID nay la cua ai.
 */
public record WalletId(UUID value) {

    public WalletId {
        Objects.requireNonNull(value, "WalletId value must not be null");
    }

    public static WalletId of(UUID value) {
        return new WalletId(value);
    }

    /** Sinh dinh danh moi khi mo vi. */
    public static WalletId random() {
        return new WalletId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
