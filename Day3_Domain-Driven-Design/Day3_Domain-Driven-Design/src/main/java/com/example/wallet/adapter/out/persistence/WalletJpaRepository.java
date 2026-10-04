package com.example.wallet.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, UUID> {
}
