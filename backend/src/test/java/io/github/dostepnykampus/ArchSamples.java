/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.ToolProvider;

/**
 * Przykłady kodu dla testów reguł architektury: źródła zapisane w katalogu, skompilowane javac
 * i zaimportowane do ArchUnit. Źródła leżą w {@code src}, klasy w {@code out} pod katalogiem
 * przykładów.
 *
 * @since 0.1.0
 */
final class ArchSamples {

    /**
     * Katalog przykładów.
     */
    private final Path dir;

    /**
     * Tworzy przykłady w katalogu.
     *
     * @param dir Katalog przykładów, zwykle tymczasowy katalog testu
     */
    ArchSamples(final Path dir) {
        this.dir = dir;
    }

    /**
     * Zapisuje plik źródłowy typu.
     *
     * @param pack Pakiet typu
     * @param name Nazwa pliku bez rozszerzenia
     * @param code Cała treść pliku, z deklaracją pakietu
     * @throws IOException Gdy zapis się nie udał
     */
    void write(final String pack, final String name, final String code) throws IOException {
        Files.writeString(
            Files.createDirectories(this.dir.resolve("src").resolve(pack.replace('.', '/')))
                .resolve(String.format("%s.java", name)),
            code
        );
    }

    /**
     * Zapisuje publiczną klasę finalną, pustą albo z jednym polem.
     *
     * @param pack Pakiet klasy
     * @param name Nazwa klasy
     * @param field Pełna nazwa typu pola; pusta, gdy klasa nie ma pola
     * @throws IOException Gdy zapis się nie udał
     */
    void type(final String pack, final String name, final String field) throws IOException {
        final String body;
        if (field.isEmpty()) {
            body = String.format("public final class %s { }", name);
        } else {
            body = String.format("public final class %s { %s field; }", name, field);
        }
        this.write(pack, name, String.format("package %s;%n%s%n", pack, body));
    }

    /**
     * Kompiluje wszystkie zapisane źródła.
     *
     * @param options Dodatkowe opcje javac, np. ścieżka klas
     * @return Katalog skompilowanych klas; przykład, który się nie kompiluje, kończy test
     *  błędem {@link IllegalStateException}
     * @throws IOException Gdy odczyt źródeł się nie udał
     */
    Path compile(final String... options) throws IOException {
        final Path out = this.dir.resolve("out");
        final List<String> args = new ArrayList<>(List.of("-d", out.toString()));
        args.addAll(List.of(options));
        try (Stream<Path> files = Files.walk(this.dir.resolve("src"))) {
            files.filter(file -> file.toString().endsWith(".java"))
                .map(Path::toString)
                .forEach(args::add);
        }
        if (ToolProvider.getSystemJavaCompiler().run(
            null, null, null, args.toArray(String[]::new)
        ) != 0) {
            throw new IllegalStateException("Przykład się nie skompilował");
        }
        return out;
    }

    /**
     * Kompiluje wszystkie zapisane źródła i importuje klasy pakietu.
     *
     * @param pack Pakiet importowanych klas, z podpakietami
     * @param options Dodatkowe opcje javac
     * @return Klasy pakietu
     * @throws IOException Gdy odczyt źródeł się nie udał
     */
    JavaClasses compiled(final String pack, final String... options) throws IOException {
        return new ClassFileImporter().importPath(
            this.compile(options).resolve(pack.replace('.', '/'))
        );
    }
}
