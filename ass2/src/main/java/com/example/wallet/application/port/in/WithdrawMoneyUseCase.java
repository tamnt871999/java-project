package com.example.wallet.application.port.in;

/**
 * INBOUND PORT - rut tien khoi vi.
 *
 * Hop dong tra loi cau hoi "ung dung nay lam duoc gi", viet duoi goc nhin
 * nghiep vu chu khong phai goc nhin ky thuat.
 *
 * Moi driving adapter (CLI, REST controller, message consumer, bo test) deu
 * goi qua interface nay va CHI qua no.
 */
public interface WithdrawMoneyUseCase {

    WalletSnapshot withdrawMoney(WithdrawMoneyCommand command);
}
