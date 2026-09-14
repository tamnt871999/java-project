package com.example.wallet.adapter.out.persistence;

import com.example.wallet.application.port.out.WalletRepository;
import com.example.wallet.domain.Money;
import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;
import com.example.wallet.domain.WalletStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DRIVEN ADAPTER - hien thuc outbound port bang bo nho trong.
 *
 * Day la file DUY NHAT trong bai biet du lieu duoc cat o dau. Doi sang JDBC,
 * JPA hay Redis chi phai viet mot adapter khac va doi mot dong o Main - khong
 * mot dong nao trong domain hay application phai sua.
 *
 * Luu y cach luu: KHONG cat thang doi tuong Wallet vao Map. Wallet kha bien,
 * neu cat tham chieu thi moi thay doi tren aggregate se am tham hien trong
 * "database" ke ca khi chua ai goi save() - mot bo nho gia nhu vay se che mat
 * loi quen goi save. Vi vay adapter tu chuyen doi qua lai voi mot ban ghi
 * rieng, dung nhu mot database that phai lam.
 */
public class InMemoryWalletRepository implements WalletRepository {

    /** Ban ghi phang - dong vai hang trong bang, khong phai aggregate. */
    private record WalletRow(BigDecimal balance, String status) {
    }

    private final Map<WalletId, WalletRow> rows = new ConcurrentHashMap<>();

    @Override
    public Optional<Wallet> findById(WalletId id) {
        return Optional.ofNullable(rows.get(id))
                .map(row -> Wallet.rehydrate(
                        id,
                        new Money(row.balance()),
                        WalletStatus.valueOf(row.status())));
    }

    @Override
    public void save(Wallet wallet) {
        rows.put(wallet.id(), new WalletRow(
                wallet.balance().amount(),
                wallet.status().name()));
    }
}
