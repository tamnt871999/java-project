package com.example.wallet.adapter.in.web;

import com.example.wallet.application.port.in.GetWalletUseCase;
import com.example.wallet.application.port.in.LockWalletUseCase;
import com.example.wallet.application.port.in.OpenWalletUseCase;
import com.example.wallet.application.port.in.OpenWalletUseCase.OpenWalletCommand;
import com.example.wallet.application.port.in.WalletSnapshot;
import com.example.wallet.application.port.in.WithdrawMoneyUseCase;
import com.example.wallet.application.port.in.WithdrawMoneyUseCase.WithdrawMoneyCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
class WalletController {

    private final OpenWalletUseCase openWalletUseCase;
    private final WithdrawMoneyUseCase withdrawMoneyUseCase;
    private final LockWalletUseCase lockWalletUseCase;
    private final GetWalletUseCase getWalletUseCase;

    WalletController(OpenWalletUseCase openWalletUseCase,
                     WithdrawMoneyUseCase withdrawMoneyUseCase,
                     LockWalletUseCase lockWalletUseCase,
                     GetWalletUseCase getWalletUseCase) {
        this.openWalletUseCase = openWalletUseCase;
        this.withdrawMoneyUseCase = withdrawMoneyUseCase;
        this.lockWalletUseCase = lockWalletUseCase;
        this.getWalletUseCase = getWalletUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    WalletSnapshot openWallet(@Valid @RequestBody OpenWalletRequest request) {
        return openWalletUseCase.openWallet(new OpenWalletCommand(request.initialBalance()));
    }

    @GetMapping("/{walletId}")
    WalletSnapshot getWallet(@PathVariable("walletId") UUID walletId) {
        return getWalletUseCase.getWallet(walletId);
    }

    @PostMapping("/{walletId}/withdraw")
    WalletSnapshot withdrawMoney(@PathVariable("walletId") UUID walletId,
                                 @Valid @RequestBody WithdrawRequest request) {
        return withdrawMoneyUseCase.withdrawMoney(
                new WithdrawMoneyCommand(walletId, request.amount()));
    }

    @PostMapping("/{walletId}/lock")
    WalletSnapshot lockWallet(@PathVariable("walletId") UUID walletId) {
        return lockWalletUseCase.lockWallet(walletId);
    }

    record OpenWalletRequest(

            @NotNull(message = "Thieu truong bat buoc: initialBalance")
            BigDecimal initialBalance) {
    }

    record WithdrawRequest(

            @NotNull(message = "Thieu truong bat buoc: amount")
            BigDecimal amount) {
    }
}
