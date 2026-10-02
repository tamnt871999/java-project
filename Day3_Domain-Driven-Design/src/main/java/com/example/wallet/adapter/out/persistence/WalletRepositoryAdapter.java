package com.example.wallet.adapter.out.persistence;

import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Money;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import com.example.wallet.domain.WalletStatus;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
class WalletRepositoryAdapter implements WalletRepository {

    private final WalletJpaRepository jpaRepository;

    WalletRepositoryAdapter(WalletJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<Wallet> findById(WalletId id) {
        return jpaRepository.findById(id.value()).map(WalletRepositoryAdapter::toDomain);
    }

    @Override
    public void save(Wallet wallet) {
        jpaRepository.save(toEntity(wallet));
    }

    private static WalletJpaEntity toEntity(Wallet wallet) {
        return new WalletJpaEntity(
                wallet.id().value(),
                wallet.balance().amount(),
                wallet.status().name());
    }

    private static Wallet toDomain(WalletJpaEntity entity) {
        return Wallet.rehydrate(
                WalletId.of(entity.getId()),
                new Money(entity.getBalance()),
                WalletStatus.valueOf(entity.getStatus()));
    }
}
