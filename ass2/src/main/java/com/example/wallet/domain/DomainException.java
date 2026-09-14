package com.example.wallet.domain;

/**
 * NGOAI LE NGHIEP VU do chinh domain nem ra - yeu cau c.
 *
 * Ma nguon cu nem "new Exception(...)" - mot ngoai le CHECKED chung chung.
 * Doi lai bang kieu rieng va UNCHECKED co ba cai loi:
 *
 *   1. Doc duoc y dinh: bat DomainException nghia la "luat nghiep vu bi vi
 *      pham", khac han voi loi ky thuat (mat ket noi, het bo nho). Bat
 *      Exception tran thi hai loai loi do lan vao nhau.
 *
 *   2. Khong lam ban chu ky ham: checked exception buoc MOI ham goi phai
 *      "throws Exception" theo, keo chuoi ngoai le lan tu domain ra toi tan
 *      controller - dung thu ma Clean Architecture muon chan.
 *
 *   3. Vi pham invariant la loi cua NGUOI GOI, khong phai tinh huong binh
 *      thuong can xu ly tung ca. De unchecked cho no noi len toi bien roi
 *      dich mot lan thanh thong bao cho nguoi dung.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
