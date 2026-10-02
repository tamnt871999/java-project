package com.example.wallet.domain;

import java.math.BigDecimal;
import java.util.Objects;

public class Wallet {

    private final WalletId id;
    private Money balance;
    private WalletStatus status;

    private Wallet(WalletId id, Money balance, WalletStatus status) {
        this.id = id;
        this.balance = balance;
        this.status = status;
    }

    public static Wallet open(WalletId id, Money initialBalance) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(initialBalance, "initialBalance must not be null");
        return new Wallet(id, initialBalance, WalletStatus.ACTIVE);
    }

    public static Wallet rehydrate(WalletId id, Money balance, WalletStatus status) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(balance, "balance must not be null");
        Objects.requireNonNull(status, "status must not be null");
        return new Wallet(id, balance, status);
    }

    public void withdrawMoney(BigDecimal amount) {
        Money requested = new Money(amount);
        if (status.isLocked()) {
            throw new DomainException("Vi dien tu hien dang bi khoa, khong the rut tien");
        }
        if (!balance.isAtLeast(requested)) {
            throw new DomainException("So du khong du: can " + requested
                    + " nhung chi con " + balance);
        }
        this.balance = balance.minus(requested);
    }

    public void lockWallet() {
        this.status = WalletStatus.LOCKED;
    }

    public WalletId id() {
        return id;
    }

    public Money balance() {
        return balance;
    }

    public WalletStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Wallet wallet && id.equals(wallet.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Wallet[id=%s, balance=%s, status=%s]".formatted(id, balance, status);
    }
}
