/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Zapis treści pliku danych w tabelach bazy w ramach migracji {@link FileMigration}.
 *
 * @since 0.1.0
 */
@FunctionalInterface
interface FileImport {

    /**
     * Zapisuje treść pliku.
     *
     * @param connection Połączenie migracji
     * @param content Bajty pliku
     * @throws SQLException Gdy zapis się nie uda
     */
    void save(Connection connection, byte[] content) throws SQLException;
}
