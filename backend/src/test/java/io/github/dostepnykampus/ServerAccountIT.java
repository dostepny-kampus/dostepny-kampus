/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Konto serwera założone skryptem {@code infra/postgis/konto-serwera.sh} nie jest
 * superużytkownikiem i nie tworzy tabel, ale czyta i zapisuje wiersze tabeli, którą właściciel
 * założył po uruchomieniu skryptu.
 *
 * <p>Wymagania: NFR-35</p>
 *
 * @since 0.1.0
 */
@Tag("kontekst-springa")
final class ServerAccountIT {

    @Test
    void limitsServerAccountToRows() throws Exception {
        try (PostgreSQLContainer postgis = ServerAccountIT.postgis()) {
            postgis.start();
            try (Connection owner = postgis.createConnection("");
                Statement ddl = owner.createStatement()) {
                ddl.execute("CREATE TABLE pozniejsza (id int)");
            }
            try (Connection server = DriverManager.getConnection(
                postgis.getJdbcUrl(), "konto_serwera", "haslo-serwera"
            ); Statement sql = server.createStatement()) {
                Assertions.assertEquals(
                    "false 1 odmowa",
                    String.format(
                        "%s %d %s", ServerAccountIT.superuser(sql),
                        sql.executeUpdate("INSERT INTO pozniejsza VALUES (1)"),
                        ServerAccountIT.create(sql)
                    ),
                    "Konto serwera jest superużytkownikiem, nie zapisuje wierszy albo tworzy tabele"
                );
            }
        }
    }

    private static String superuser(final Statement sql) throws SQLException {
        try (ResultSet row = sql.executeQuery(
            "SELECT rolsuper FROM pg_roles WHERE rolname = current_user"
        )) {
            row.next();
            return Boolean.toString(row.getBoolean(1));
        }
    }

    private static String create(final Statement sql) {
        String result = "utworzona";
        try {
            sql.execute("CREATE TABLE obca (id int)");
        } catch (final SQLException ex) {
            result = "odmowa";
        }
        return result;
    }

    private static PostgreSQLContainer postgis() throws IOException {
        final PostgreSQLContainer postgis = new PostgreSQLContainer(
            PostgisTestConfiguration.image()
        );
        postgis.withEnv("DK_DB_APP_USER", "konto_serwera")
            .withEnv("DK_DB_APP_PASSWORD", "haslo-serwera")
            .withCopyFileToContainer(
                MountableFile.forHostPath(Path.of("../infra/postgis/konto-serwera.sh"), 0755),
                "/docker-entrypoint-initdb.d/konto-serwera.sh"
            );
        return postgis;
    }
}
