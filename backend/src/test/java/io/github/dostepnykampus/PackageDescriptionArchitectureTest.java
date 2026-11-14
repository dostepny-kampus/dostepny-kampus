/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Strażnik opisu pakietów serwera: każdy katalog kodu produkcyjnego z klasami ma
 * {@code package-info.java}, a jego Javadoc zaczyna się zdaniem opisu pakietu. Treść opisu
 * (tylko kontrakt) sprawdza {@link JavadocArchitectureTest}.
 *
 * @since 0.1.0
 */
final class PackageDescriptionArchitectureTest {

    /**
     * Pierwsze zdanie Javadoc: tekst przed pierwszą kropką, bez znaczników.
     */
    private static final Pattern SENTENCE = Pattern.compile(
        "/\\*\\*\\s*\\*\\s*([^@<\\s][^.]*\\.)", Pattern.DOTALL
    );

    /**
     * Katalog na przykłady, nowy w każdym teście.
     */
    @TempDir
    private Path dir;

    @Test
    void everyProductionPackageHasDescription() throws IOException {
        Assertions.assertEquals(
            List.of(),
            PackageDescriptionArchitectureTest.undescribed(Path.of("src", "main", "java")),
            "Pakiet nie ma package-info.java ze zdaniem opisu"
        );
    }

    @Test
    void rejectsPackageWithoutInfoAndWithEmptyDescription() throws IOException {
        this.file("a.bare", "Model.java", "package a.bare;", "final class Model { }");
        this.file("a.empty", "Model.java", "package a.empty;", "final class Model { }");
        this.file(
            "a.empty", "package-info.java", "/**", " * @since 0.1.0", " */", "package a.empty;"
        );
        this.file("a.good", "Model.java", "package a.good;", "final class Model { }");
        this.file(
            "a.good", "package-info.java",
            "/**", " * Zgłoszenie bariery.", " *", " * @since 0.1.0", " */", "package a.good;"
        );
        Assertions.assertEquals(
            List.of("a/bare", "a/empty"),
            PackageDescriptionArchitectureTest.undescribed(this.dir),
            "Reguła przepuściła pakiet bez opisu albo odrzuciła pakiet z opisem"
        );
    }

    private void file(final String pack, final String name, final String... lines)
        throws IOException {
        Files.writeString(
            Files.createDirectories(this.dir.resolve(pack.replace('.', '/'))).resolve(name),
            String.join(System.lineSeparator(), lines)
        );
    }

    private static List<String> undescribed(final Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files
                .filter(file -> file.toString().endsWith(".java"))
                .filter(file -> !"package-info.java".equals(file.getFileName().toString()))
                .map(Path::getParent)
                .distinct()
                .filter(pack -> !PackageDescriptionArchitectureTest.described(pack))
                .map(pack -> root.relativize(pack).toString().replace('\\', '/'))
                .sorted()
                .toList();
        }
    }

    private static boolean described(final Path pack) {
        final Path info = pack.resolve("package-info.java");
        boolean result = false;
        if (Files.exists(info)) {
            try {
                result = PackageDescriptionArchitectureTest.SENTENCE
                    .matcher(Files.readString(info)).find();
            } catch (final IOException ex) {
                throw new IllegalStateException(String.format("Brak odczytu %s", info), ex);
            }
        }
        return result;
    }
}
