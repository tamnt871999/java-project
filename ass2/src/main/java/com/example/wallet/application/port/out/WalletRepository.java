package com.example.wallet.application.port.out;

import com.example.wallet.domain.Wallet;
import com.example.wallet.domain.WalletId;

import java.util.Optional;

/**
 * OUTBOUND PORT - cua ngo di ra tang luu tru.
 *
 * DAY LA DIEM MAU CHOT CUA CLEAN ARCHITECTURE:
 *
 * Interface nay do tang APPLICATION so huu, con class hien thuc no
 * (InMemoryWalletRepository) nam o tang ADAPTER ben ngoai. Nho vay luc CHAY
 * thi use case goi ra ngoai, nhung luc BIEN DICH thi mui ten phu thuoc van chi
 * VAO TRONG. Do la Dependency Inversion.
 *
 * Chu y chu ky ham: nhan va tra ve Wallet cua DOMAIN, tuyet doi khong nhac toi
 * ResultSet, JPA hay SQL.
 *
 * findById tra ve Optional chu khong nem ngoai le: "khong tim thay vi" la mot
 * ket qua tra cuu binh thuong, khong phai su co cua tang luu tru. Quyet dinh
 * coi do la loi hay khong thuoc ve use case.
 */
public interface WalletRepository {

    Optional<Wallet> findById(WalletId id);

    void save(Wallet wallet);
}
