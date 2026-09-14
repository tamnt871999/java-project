package com.example.wallet.application.port.in;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * DTO BIEN - du lieu di VAO use case rut tien.
 *
 * Chi chua kieu nguyen thuy: day la hop dong voi the gioi ben ngoai, khong nen
 * bat adapter phai biet WalletId hay Money cua domain moi goi duoc use case.
 *
 * Command tu kiem tra CU PHAP ngay tai constructor (co mat truong khong).
 * Con kiem tra NGHIEP VU - so tien co hop le khong, vi co du so du khong - la
 * viec cua aggregate, khong phai cua DTO.
 */
public record WithdrawMoneyCommand(UUID walletId, BigDecimal amount) {

    public WithdrawMoneyCommand {
        Objects.requireNonNull(walletId, "Thieu walletId");
        Objects.requireNonNull(amount, "Thieu so tien can rut");
    }
}
