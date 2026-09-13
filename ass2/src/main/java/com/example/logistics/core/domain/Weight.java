package com.example.logistics.core.domain;

/**
 * Trong luong kien hang, don vi chuan cua loi la GRAM.
 *
 * Vi sao phai co kieu rieng thay vi truyen thang so int?
 *   - Moi doi tac dung mot don vi khac nhau: GHN nhan gram, GHTK nhan kilogram.
 *     Neu loi truyen "int weight" tran lan thi som muon cung co cho nham don vi.
 *   - Rang buoc "phai lon hon 0" duoc kiem MOT lan tai day, khong phai kiem lai
 *     o moi Use Case.
 *
 * Viec quy doi gram -> kg la trach nhiem cua Adapter, khong phai cua loi.
 */
public class Weight implements Comparable<Weight> {

    /** Gioi han ky thuat de chan du lieu ro rang la rac (100 kg). */
    private static final long MAX_GRAMS = 100_000L;

    private final long grams;

    public Weight(long grams) {
        if (grams <= 0) {
            throw new DomainException("Trong luong phai lon hon 0 gram, nhan duoc: " + grams);
        }
        if (grams > MAX_GRAMS) {
            throw new DomainException("Trong luong vuot qua 100kg: " + grams + "g");
        }
        this.grams = grams;
    }

    public static Weight ofGrams(long grams) {
        return new Weight(grams);
    }

    public static Weight ofKilograms(double kilograms) {
        return new Weight(Math.round(kilograms * 1000));
    }

    public long grams() {
        return grams;
    }

    public double toKilograms() {
        return grams / 1000.0;
    }

    @Override
    public int compareTo(Weight other) {
        return Long.compare(this.grams, other.grams);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Weight w && grams == w.grams;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(grams);
    }

    @Override
    public String toString() {
        return grams + "g";
    }
}
