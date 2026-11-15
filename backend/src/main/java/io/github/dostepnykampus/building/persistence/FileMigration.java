/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.SQLException;
import java.util.zip.CRC32;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;

/**
 * Powtarzalna migracja Flyway z pliku danych dostarczonego z serwerem: działa przy pierwszym
 * starcie i po zmianie treści pliku albo wersji importu, a treść zapisuje {@link FileImport}
 * w transakcji migracji.
 *
 * @since 0.1.0
 */
final class FileMigration implements JavaMigration {

    /**
     * Opis migracji w historii Flyway.
     */
    private final String description;

    /**
     * Plik danych.
     */
    private final Resource file;

    /**
     * Wersja importu: zmiana wykonuje import ponownie także bez zmiany pliku.
     */
    private final int format;

    /**
     * Zapis treści pliku.
     */
    private final FileImport step;

    /**
     * Migracja z pliku.
     *
     * @param description Opis migracji w historii Flyway
     * @param file Plik danych
     * @param format Wersja importu
     * @param step Zapis treści pliku
     */
    FileMigration(
        final String description, final Resource file, final int format, final FileImport step
    ) {
        this.description = description;
        this.file = file;
        this.format = format;
        this.step = step;
    }

    @Override
    public @Nullable MigrationVersion getVersion() {
        return null;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public Integer getChecksum() {
        final CRC32 crc = new CRC32();
        crc.update(this.content());
        crc.update(this.format);
        return (int) crc.getValue();
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(final Context context) throws SQLException {
        this.step.save(context.getConnection(), this.content());
    }

    private byte[] content() {
        try {
            return this.file.getContentAsByteArray();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
