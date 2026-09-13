package com.example.logistics;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * FITNESS FUNCTION - bai test canh giu kien truc.
 *
 * VAN DE NO GIAI QUYET: luat kien truc viet trong README thi khong ai bat buoc
 * phai doc. Sau vai sprint, mot nguoi dang voi chi can go "import com.example.
 * logistics.infrastructure..." vao Use Case cho nhanh la ca kien truc Hexagonal
 * am tham sup do, ma build van xanh va test nghiep vu van qua het.
 *
 * Lop nay bien luat kien truc thanh mot bai test chay cung CI: no doc thang ma
 * nguon trong src/main/java va bao do neu co file nao vuot ranh gioi. Luat
 * duoc THUC THI chu khong chi duoc ghi chep.
 *
 * Ba luat dang canh:
 *   1. core KHONG duoc phu thuoc infrastructure / bootstrap  (Dependency Rule)
 *   2. core/domain KHONG duoc phu thuoc port / usecase / factory
 *      (domain la vong trong cung, khong biet gi ve cac vong ngoai no)
 *   3. core KHONG duoc dinh cong nghe ha tang (java.net, java.sql, javax, ...)
 *
 * Doc ma nguon bang chuoi la cach lam don gian nhat, du dung cho bai tap. Du an
 * that nen dung ArchUnit - no phan tich bytecode nen bat duoc ca truong hop
 * dung ten day du khong qua import.
 */
final class ArchitectureFitness {

    private static final String PKG = "com.example.logistics";

    /** Cac thu muc duoc coi la "loi" - noi phai sach nhat. */
    private static final String CORE = "core";
    private static final String DOMAIN = "core/domain";

    static void run() {
        Path sourceRoot = locateSourceRoot();
        if (sourceRoot == null) {
            SelfCheck.fail("fitness function: khong tim thay src/main/java"
                    + " - hay chay bang .\\run.ps1 test");
            return;
        }
        loiKhongPhuThuocTangNgoai(sourceRoot);
        domainKhongPhuThuocVongNgoai(sourceRoot);
        loiKhongDinhCongNgheHaTang(sourceRoot);
    }

    // ------------------------------------------------------------------ luat 1

    /** Dependency Rule: mui ten phu thuoc chi duoc huong tu ngoai vao trong. */
    private static void loiKhongPhuThuocTangNgoai(Path sourceRoot) {
        List<String> viPham = quetViPham(sourceRoot.resolve(PKG.replace('.', '/')).resolve(CORE),
                PKG + ".infrastructure", PKG + ".bootstrap");
        bao("core khong phu thuoc infrastructure / bootstrap", viPham);
    }

    // ------------------------------------------------------------------ luat 2

    /** Domain la vong trong cung: no khong duoc biet den port, usecase hay factory. */
    private static void domainKhongPhuThuocVongNgoai(Path sourceRoot) {
        List<String> viPham = quetViPham(sourceRoot.resolve(PKG.replace('.', '/')).resolve(DOMAIN),
                PKG + ".core.port", PKG + ".core.usecase", PKG + ".core.factory");
        bao("core/domain khong phu thuoc port / usecase / factory", viPham);
    }

    // ------------------------------------------------------------------ luat 3

    /**
     * Loi chi duoc dung cac kieu du lieu thuan tuy cua ngon ngu.
     *
     * Thay mot dong "import java.sql.Connection" trong Use Case la dau hieu ro
     * nhat cua viec ha tang dang bo vao loi.
     */
    private static void loiKhongDinhCongNgheHaTang(Path sourceRoot) {
        List<String> viPham = quetViPham(sourceRoot.resolve(PKG.replace('.', '/')).resolve(CORE),
                "java.net.", "java.sql.", "java.io.", "javax.", "jakarta.",
                "org.springframework.", "com.fasterxml.");
        bao("core khong dinh cong nghe ha tang (net / sql / io / spring / json)", viPham);
    }

    // ------------------------------------------------------------------ ha tang

    /**
     * Tim moi dong code (bo qua comment) co chua mot trong cac chuoi bi cam.
     *
     * Phai bo qua comment vi chinh cac file trong core co giai thich bang loi
     * van tai sao chung KHONG duoc import infrastructure - neu khong loai tru,
     * bai test se bao do vi chinh loi giai thich cua no.
     */
    private static List<String> quetViPham(Path dir, String... chuoiBiCam) {
        List<String> viPham = new ArrayList<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (laComment(line)) {
                        continue;
                    }
                    for (String cam : chuoiBiCam) {
                        if (line.contains(cam)) {
                            viPham.add(file.getFileName() + ":" + (i + 1) + " -> " + line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc ma nguon tai " + dir, e);
        }
        return viPham;
    }

    private static boolean laComment(String trimmedLine) {
        return trimmedLine.startsWith("//")
                || trimmedLine.startsWith("*")
                || trimmedLine.startsWith("/*");
    }

    private static void bao(String tenLuat, List<String> viPham) {
        if (viPham.isEmpty()) {
            SelfCheck.pass(tenLuat);
        } else {
            SelfCheck.fail(tenLuat + " - co " + viPham.size() + " vi pham:");
            viPham.forEach(v -> System.out.println("           " + v));
        }
    }

    /**
     * Tim src/main/java. run.ps1 truyen -Dproject.root, con khi chay tu IDE thi
     * do dan len tu thu muc lam viec hien tai.
     */
    private static Path locateSourceRoot() {
        List<Path> ungVien = new ArrayList<>();
        String declared = System.getProperty("project.root");
        if (declared != null && !declared.isBlank()) {
            ungVien.add(Path.of(declared, "src", "main", "java"));
        }
        ungVien.add(Path.of("src", "main", "java"));
        ungVien.add(Path.of("ass2", "src", "main", "java"));
        for (Path p : ungVien) {
            if (Files.isDirectory(p)) {
                return p;
            }
        }
        return null;
    }

    private ArchitectureFitness() {
    }
}
