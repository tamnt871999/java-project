package com.example.logistics.core.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Tinh / thanh pho nhan hang.
 *
 * Nguoi dung go "TP. Ho Chi Minh", "ho chi minh", "HCM", "Sai Gon" deu la mot
 * noi. Neu de nguyen chuoi tho roi so sanh bang equals() thi luat dinh tuyen
 * nha van chuyen se sai ngay o ky tu hoa thuong dau tien.
 *
 * Vi vay City giu 2 thu:
 *   - displayName : chuoi goc de hien thi lai cho nguoi dung.
 *   - key         : chuoi da chuan hoa (khong dau, chu thuong, bo tien to
 *                   "TP."/"Tinh") - DAY moi la thu dung de so sanh va dinh tuyen.
 */
public final class City {

    /** Vai bi danh pho bien. Them bi danh moi khong lam thay doi luat dinh tuyen. */
    private static final Map<String, String> ALIASES = Map.of(
            "hcm", "ho chi minh",
            "tphcm", "ho chi minh",
            "sai gon", "ho chi minh",
            "saigon", "ho chi minh",
            "hn", "ha noi",
            "dn", "da nang",
            "hue", "thua thien hue");

    private final String displayName;
    private final String key;

    private City(String displayName, String key) {
        this.displayName = displayName;
        this.key = key;
    }

    public static City of(String rawName) {
        Objects.requireNonNull(rawName, "city must not be null");
        String display = rawName.trim();
        if (display.isEmpty()) {
            throw new DomainException("Vui long nhap tinh / thanh pho nhan hang");
        }
        return new City(display, normalize(display));
    }

    public String displayName() {
        return displayName;
    }

    /** Khoa chuan hoa - dung lam khoa tra bang trong luat dinh tuyen. */
    public String key() {
        return key;
    }

    /**
     * "TP. Ho Chi Minh" -> "ho chi minh";  "Da Nang" (co dau) -> "da nang".
     *
     * Buoc "d gach ngang -> d" phai lam THU CONG vi Normalizer.NFD khong tach
     * duoc ky tu nay thanh chu cai + dau.
     */
    private static String normalize(String raw) {
        String s = raw.toLowerCase(Locale.ROOT).replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("[^a-z0-9]+", " ").trim();
        s = s.replaceAll("^(tp|thanh pho|tinh)\\s+", "");
        return ALIASES.getOrDefault(s, s);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof City c && key.equals(c.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
