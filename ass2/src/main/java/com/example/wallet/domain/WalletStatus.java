package com.example.wallet.domain;

/**
 * Trang thai vi dien tu.
 *
 * Ma nguon cu dung "public String status" voi ghi chu // "ACTIVE", "LOCKED".
 * Ghi chu khong phai la rang buoc: khong gi ngan duoc ai do gan status =
 * "Locked", "LOCK" hay "dang khoa", va phep so sanh
 * "LOCKED".equals(wallet.status) se am tham tra ve false - vi khoa mat tac
 * dung ma khong bao loi.
 *
 * Doi sang enum thi tap gia tri hop le duoc COMPILER canh, khong con phu
 * thuoc vao viec lap trinh vien go dung chuoi.
 */
public enum WalletStatus {

    ACTIVE,
    LOCKED;

    public boolean isLocked() {
        return this == LOCKED;
    }
}
