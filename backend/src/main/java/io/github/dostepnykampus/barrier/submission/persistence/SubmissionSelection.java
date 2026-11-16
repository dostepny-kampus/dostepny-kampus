/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.persistence;

import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Warunek SQL wyboru zgłoszeń według zapytania listy i jego parametry: statusy,
 * opcjonalny budynek, piętro i autor. Wspólny dla listy i stron listy.
 *
 * <p>Wymagania: US1.3, US8.1, NFR-34</p>
 *
 * @since 0.1.0
 */
final class SubmissionSelection {

    /**
     * Warunek {@code WHERE} z parametrami statuses, building, floor i author.
     */
    static final String WHERE = """
        status IN (:statuses)
            AND (CAST(:building AS TEXT) IS NULL OR building_symbol = :building)
            AND (CAST(:floor AS TEXT) IS NULL OR floor_id = :floor)
            AND (CAST(:author AS TEXT) IS NULL OR author = :author)""";

    /**
     * Klasa ze stałą i jedną funkcją, bez instancji.
     */
    private SubmissionSelection() {
    }

    /**
     * Wiąże parametry warunku {@link #WHERE}.
     *
     * @param sql Zapytanie z warunkiem
     * @param query Statusy oraz opcjonalny budynek i piętro
     * @param author Identyfikator konta autora albo null dla wszystkich kont
     * @return Zapytanie z parametrami warunku
     */
    static JdbcClient.StatementSpec bind(
        final JdbcClient.StatementSpec sql, final SubmissionQuery query,
        final @Nullable String author
    ) {
        return sql.param("statuses", query.statuses().stream().map(Enum::name).toList())
            .param("building", query.buildingSymbol())
            .param("floor", query.floorId())
            .param("author", author);
    }
}
