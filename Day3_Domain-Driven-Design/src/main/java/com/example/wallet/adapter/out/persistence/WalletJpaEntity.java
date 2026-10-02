package com.example.wallet.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "wallets")
class WalletJpaEntity {

    @Id
    private UUID id;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "status", nullable = false)
    private String status;

    protected WalletJpaEntity() {
    }

    WalletJpaEntity(UUID id, BigDecimal balance, String status) {
        this.id = id;
        this.balance = balance;
        this.status = status;
    }

    UUID getId() {
        return id;
    }

    BigDecimal getBalance() {
        return balance;
    }

    String getStatus() {
        return status;
    }
}
