package com.example.ordering.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Fitness function - ranh gioi giua cac tang")
class ArchitectureFitnessTest {

    private static final String PKG = "com.example.ordering";
    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    @Test
    @DisplayName("domain khong phu thuoc application hay adapter")
    void domainKhongBietVongNgoai() {
        assertKhongImport("domain", List.of(PKG + ".application", PKG + ".adapter"));
    }

    @Test
    @DisplayName("domain khong dinh cong nghe ha tang")
    void domainSachKhoiHaTang() {
        assertKhongImport("domain", List.of(
                "org.springframework", "jakarta.", "javax.", "com.fasterxml", "java.sql"));
    }

    @Test
    @DisplayName("application khong phu thuoc adapter")
    void applicationKhongBietAdapter() {
        assertKhongImport("application", List.of(PKG + ".adapter"));
    }

    @Test
    @DisplayName("adapter web khong phu thuoc adapter persistence")
    void haiAdapterKhongBietNhau() {
        assertKhongImport("adapter/in", List.of(PKG + ".adapter.out"));
    }

    private static void assertKhongImport(String layer, List<String> forbiddenPrefixes) {
        Path layerRoot = SOURCE_ROOT.resolve(PKG.replace('.', '/')).resolve(layer);
        assertThat(layerRoot).as("khong tim thay thu muc %s", layerRoot).exists();

        List<String> viPham = new ArrayList<>();
        try (Stream<Path> files = Files.walk(layerRoot)) {
            files.filter(file -> file.toString().endsWith(".java"))
                    .forEach(file -> viPham.addAll(timImportCam(file, forbiddenPrefixes)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertThat(viPham)
                .as("tang '%s' khong duoc phu thuoc %s", layer, forbiddenPrefixes)
                .isEmpty();
    }

    private static List<String> timImportCam(Path file, List<String> forbiddenPrefixes) {
        List<String> viPham = new ArrayList<>();
        for (String line : docDong(file)) {
            String trimmed = line.strip();
            if (!trimmed.startsWith("import ")) {
                continue;
            }
            String imported = trimmed.substring("import ".length());
            for (String forbidden : forbiddenPrefixes) {
                if (imported.startsWith(forbidden)) {
                    viPham.add(file.getFileName() + " -> " + trimmed);
                }
            }
        }
        return viPham;
    }

    private static List<String> docDong(Path file) {
        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
