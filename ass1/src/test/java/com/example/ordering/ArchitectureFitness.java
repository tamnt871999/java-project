package com.example.ordering;

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
 * vao Entities cho nhanh la Clean Architecture am tham sup do, ma build van
 * xanh va test nghiep vu van qua het.
 *
 * BA PACKAGE, BON VONG:
 *
 *   adapter/lib   Vong 4  Frameworks and Drivers - gia lap Spring Data JPA va H2.
 *                         Trong du an that day la dependency trong pom.xml,
 *                         khong phai ma nguon, nen no khong thanh package rieng.
 *   adapter       Vong 3  Interface Adapters - Controller, Repository Adapter
 *   application   Vong 2  Use Cases - inbound port, interactor, outbound port
 *   domain        Vong 1  Entities - aggregate, value object, domain service
 *
 * THE DEPENDENCY RULE: ma nguon o vong trong KHONG duoc biet gi ve vong ngoai.
 *
 * Ghi chu ve vong 3: adapter duoc phep cham vao framework, vi do dung la viec
 * cua no. Cai bi cam tuyet doi la domain va application cham vao ha tang.
 *
 * Doc ma nguon bang chuoi la cach don gian nhat, du dung cho bai tap. Du an
 * that nen dung ArchUnit - no phan tich bytecode nen bat duoc ca truong hop
 * dung ten day du khong qua import.
 */
final class ArchitectureFitness {

    private static final String PKG = "com.example.ordering";

    static void run() {
        Path sourceRoot = locateSourceRoot();
        if (sourceRoot == null) {
            SelfCheck.fail("fitness function: khong tim thay src/main/java");
            return;
        }

        // Luat 1 - Entities la vong trong cung: khong biet gi ve moi vong ngoai.
        checkNoImport(sourceRoot, "domain", List.of(
                PKG + ".application",
                PKG + ".adapter"));

        // Luat 2 - Use Cases chi duoc biet Entities.
        checkNoImport(sourceRoot, "application", List.of(
                PKG + ".adapter"));

        // Luat 3 - hai vong trong cung phai sach bong khoi cong nghe ha tang.
        checkNoImport(sourceRoot, "domain", List.of(
                "org.springframework", "jakarta.", "javax.", "com.fasterxml",
                "com.sun.", "java.sql", "java.net"));
        checkNoImport(sourceRoot, "application", List.of(
                "org.springframework", "jakarta.", "javax.", "com.fasterxml",
                "com.sun.", "java.sql", "java.net"));

        // Luat 4 - adapter/lib la THU VIEN GIA LAP, phai tong quat that su:
        // no khong duoc biet Order, OrderEntity hay bat ky thu gi cua du an nay.
        // Ngay khi mot file trong do import PKG la no da thoi la thu vien, va
        // viec "xoa ca folder roi thay bang dependency that" khong con lam duoc.
        checkNoImport(sourceRoot, "adapter/lib", List.of(PKG + "."));
    }

    private static void checkNoImport(Path sourceRoot, String layer, List<String> forbidden) {
        Path layerRoot = sourceRoot.resolve(PKG.replace('.', '/')).resolve(layer);
        List<String> violations = new ArrayList<>();

        try (Stream<Path> files = Files.walk(layerRoot)) {
            files.filter(path -> path.toString().endsWith(".java")).forEach(path -> {
                for (String line : readLines(path)) {
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
        candidates.add(Path.of("../src/main/java"));

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
