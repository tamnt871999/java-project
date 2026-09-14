package com.example.wallet.application.usecase;

import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.domain.Wallet;

/**
 * Dich aggregate sang DTO bien.
 *
 * De o tang Application chu khong de thanh method tren chinh Wallet: neu
 * aggregate biet cach tu bien minh thanh WalletSnapshot thi domain da phu
 * thuoc nguoc len tang ngoai no, pha vo Dependency Rule.
 */
final class WalletSnapshots {

    static WalletSnapshot of(Wallet wallet) {
        return new WalletSnapshot(
                wallet.id().value(),
                wallet.balance().amount(),
                wallet.status().name());
    }

    private WalletSnapshots() {
    }
}
