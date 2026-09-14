package com.example.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO BIEN - du lieu di RA khoi use case.
 *
 * Co y KHONG tra thang aggregate Wallet ra ngoai. Neu tra, tang ngoai se goi
 * duoc withdrawMoney() / lockWallet() truc tiep tren doi tuong do, di vong qua
 * use case va qua tang luu tru - tuc la vua dong goi xong lai mo toang ra.
 *
 * Ca hai use case deu tra ve cung mot hinh dang: sau khi thao tac xong, ben
 * goi can biet vi hien con bao nhieu va dang o trang thai nao.
 */
public record WalletSnapshot(UUID walletId, BigDecimal balance, String status) {

    public WalletSnapshot {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
