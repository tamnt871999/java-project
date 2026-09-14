package com.example.wallet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * FITNESS FUNCTION - bien luat kien truc thanh bai test chay duoc.
 *
 * VAN DE NO GIAI QUYET: luat viet trong tai lieu thi khong ai bat buoc phai
 * doc. Sau vai sprint, chi can mot nguoi dang voi go "import jakarta.persistence"
 * vao aggregate cho nhanh la Clean Architecture am tham sup do, ma build van
 * xanh va test nghiep vu van qua het.
 *
 * BA VONG cua bai nay:
 *
 *   Vong 3  adapter      (Interface Adapters: hien thuc outbound port)
 *   Vong 2  application  (Use Cases: inbound port, interactor, outbound port)
 *   Vong 1  domain       (Entities: aggregate root, value object)
 *
 * Bai nay khong co vong "infrastructure" rieng vi khong dung framework nao -
 * them mot package rong chi de cho du bon vong la hinh thuc.
 *
 * THE DEPENDENCY RULE: ma nguon o vong trong KHONG duoc biet gi ve vong ngoai.
 *
 * Doc ma nguon bang chuoi la cach don gian nhat, du dung cho bai tap. Du an
 * that nen dung ArchUnit - no phan tich bytecode nen bat duoc ca truong hop
 * dung ten day du khong qua import.
 */
final class ArchitectureFitness {

    private static final String PKG = "com.example.wallet";

    static void run() {
        Path sourceRoot = locateSourceRoot();
        if (sourceRoot == null) {
            SelfCheck.fail("fitness function: khong tim thay src/main/java"
                    + " - hay chay bang .\\run.ps1 test");
            return;
        }

        // Luat 1 - Entities la vong trong cung: khong biet gi ve moi vong ngoai.
        checkNoImport(sourceRoot, "domain", List.of(
                PKG + ".application",
                PKG + ".adapter",
                PKG + ".bootstrap"));

        // Luat 2 - Use Cases chi duoc biet Entities.
        checkNoImport(sourceRoot, "application", List.of(
                PKG + ".adapter",
                PKG + ".bootstrap"));

        // Luat 3 - hai vong trong cung phai sach bong khoi cong nghe ha tang.
        List<String> haTang = List.of(
                "org.springframework", "jakarta.", "javax.", "com.fasterxml",
                "com.sun.", "java.sql", "java.net", "java.io");
        checkNoImport(sourceRoot, "domain", haTang);
        checkNoImport(sourceRoot, "application", haTang);

        // Luat 4 - chi composition root duoc phep biet ban hien thuc cu the.
        checkNoImport(sourceRoot, "adapter", List.of(PKG + ".bootstrap"));
    }

    private static void checkNoImport(Path sourceRoot, String layer, List<String> forbidden) {
        Path layerRoot = sourceRoot.resolve(PKG.replace('.', '/')).resolve(layer);
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(layerRoot)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                for (String line : readLines(path)) {
                    // Chi soi dong import: cac file trong domain co giai thich
                    // bang loi van tai sao chung KHONG import ha tang, neu xet
                    // ca comment thi bai test se do vi chinh loi giai thich do.
                    if (!line.startsWith("import ")) {
                        continue;
                    }
                    for (String prefix : forbidden) {
                        if (line.contains(prefix)) {
                            violations.add(path.getFileName() + " -> " + line.trim());
                        }
                    }
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String name = "tang '" + layer + "' khong phu thuoc " + String.join(", ", forbidden);
        if (violations.isEmpty()) {
            SelfCheck.pass(name);
        } else {
            SelfCheck.fail(name + " - vi pham: " + String.join(" | ", violations));
        }
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Tim src/main/java du bo test duoc chay tu thu muc nao. */
    private static Path locateSourceRoot() {
        String configured = System.getProperty("project.root");
        List<Path> candidates = new ArrayList<>();
        if (configured != null) {
            candidates.add(Path.of(configured, "src/main/java"));
        }
        candidates.add(Path.of("src/main/java"));
        candidates.add(Path.of("ass2/src/main/java"));

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private ArchitectureFitness() {
    }
}
