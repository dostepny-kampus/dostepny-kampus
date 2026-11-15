/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

/**
 * Suma kontrolna powtarzalnej migracji z pliku zależy od treści pliku i wersji importu
 * i ma tę samą wartość co przed wydzieleniem migracji, więc Flyway nie powtarza importu
 * bez zmiany danych (ADR-0008).
 *
 * <p>Wymagania: US1, US4.9</p>
 *
 * @since 0.1.0
 */
final class FileMigrationTest {

    @Test
    void keepsChecksumOfEarlierImport() {
        MatcherAssert.assertThat(
            "Suma kontrolna różni się od sumy z historii migracji",
            FileMigrationTest.migration("{}", 5).getChecksum(),
            Matchers.equalTo(1_293_856_151)
        );
    }

    @Test
    void changesChecksumWhenImportFormatChanges() {
        MatcherAssert.assertThat(
            "Zmiana wersji importu nie zmienia sumy kontrolnej",
            FileMigrationTest.migration("{}", 5).getChecksum(),
            Matchers.not(FileMigrationTest.migration("{}", 6).getChecksum())
        );
    }

    @Test
    void changesChecksumWhenFileChanges() {
        MatcherAssert.assertThat(
            "Zmiana treści pliku nie zmienia sumy kontrolnej",
            FileMigrationTest.migration("{}", 5).getChecksum(),
            Matchers.not(FileMigrationTest.migration("[]", 5).getChecksum())
        );
    }

    @Test
    void runsWithoutVersion() {
        MatcherAssert.assertThat(
            "Migracja z pliku ma wersję, więc nie jest powtarzalna",
            FileMigrationTest.migration("{}", 5).getVersion(),
            Matchers.nullValue()
        );
    }

    private static FileMigration migration(final String content, final int format) {
        return new FileMigration(
            "dane testowe",
            new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8)),
            format,
            (connection, bytes) -> { }
        );
    }
}
