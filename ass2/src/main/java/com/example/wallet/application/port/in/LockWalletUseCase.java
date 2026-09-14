package com.example.wallet.application.port.in;

import java.util.UUID;

/**
 * INBOUND PORT - khoa vi.
 *
 * Tach rieng khoi WithdrawMoneyUseCase chu khong gop thanh mot interface
 * "WalletService" co hai method: mot driving adapter chi can khoa vi thi khong
 * viec gi phai phu thuoc vao ca kha nang rut tien (Interface Segregation).
 *
 * Khong co Command DTO o day vi chi co dung mot tham so - boc mot UUID vao mot
 * record rieng chi de cho "cho doi xung" la nghi thuc thua.
 */
public interface LockWalletUseCase {

    WalletSnapshot lockWallet(UUID walletId);
}
