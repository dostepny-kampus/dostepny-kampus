/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.building.Outlines;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Zapis i odczyt zgłoszeń w tabeli submissions (US4, US7, BR-9).
 *
 * @since 0.1.0
 */
@Service
final class SubmissionStore {

    /**
     * Odczyt zgłoszenia z polami nadanymi przez serwer.
     */
    private static final String SELECT = """
        SELECT id::text AS id, version, status, created_at, content::text AS content
        FROM submissions
        """;

    /**
     * Dostęp do tabeli submissions.
     */
    private final JdbcClient jdbc;

    /**
     * Zapis i odczyt treści zgłoszenia jako JSON.
     */
    private final ObjectMapper mapper;

    /**
     * Kontury budynków do budynku zgłoszenia szybkiego (BR-14).
     */
    private final Outlines outlines;

    SubmissionStore(final JdbcClient jdbc, final ObjectMapper mapper, final Outlines outlines) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.outlines = outlines;
    }

    /**
     * Zapisuje nowe zgłoszenie ze statusem „wysłane” albo zwraca zapisane
     * wcześniej zgłoszenie o tym samym identyfikatorze (BR-9).
     *
     * @param draft Sprawdzone zgłoszenie z telefonu
     * @return Zgłoszenie z serwera i informacja, czy powstało teraz
     */
    Saved create(final SubmissionDraft draft) {
        final ObjectNode content = draft.content();
        if ("QUICK".equals(draft.kind())) {
            content.remove("buildingSymbol");
            this.outlines.containing(draft.lat(), draft.lon())
                .ifPresent(symbol -> content.put("buildingSymbol", symbol));
        }
        final int added;
        try {
            added = this.jdbc.sql(
                """
                INSERT INTO submissions (id, kind, building_symbol, location, content)
                VALUES (:id, :kind, :building,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), CAST(:content AS JSONB))
                ON CONFLICT (id) DO NOTHING
                """
            ).param("id", draft.id())
                .param("kind", draft.kind())
                .param("building", content.path("buildingSymbol").stringValue(null))
                .param("lat", draft.lat())
                .param("lon", draft.lon())
                .param("content", this.mapper.writeValueAsString(content))
                .update();
        } catch (final DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Nieznany budynek zgłoszenia", ex
            );
        }
        return new Saved(added == 1, this.submission(draft.id()));
    }

    /**
     * Zwraca zgłoszenie przyjęte, które widzi każdy (BR-6).
     *
     * @param id Identyfikator zgłoszenia
     * @return Zgłoszenie; brak, gdy nie ma go albo nie jest przyjęte
     */
    Optional<JsonNode> accepted(final UUID id) {
        return this.jdbc.sql(
            SubmissionStore.SELECT.concat("WHERE id = :id AND status = 'ACCEPTED'")
        ).param("id", id)
            .query((rows, num) -> this.node(rows))
            .optional();
    }

    /**
     * Zwraca zgłoszenia przyjęte do mapy i karty budynku (US1.3, US2.2, BR-6).
     *
     * @param building Symbol budynku albo null dla wszystkich budynków
     * @param floor Piętro budynku szczegółowego albo null dla wszystkich pięter
     * @return Zgłoszenia przyjęte od najstarszego
     */
    List<JsonNode> accepted(final String building, final String floor) {
        return this.jdbc.sql(
            SubmissionStore.SELECT.concat(
                """
                WHERE status = 'ACCEPTED'
                    AND (CAST(:building AS TEXT) IS NULL OR building_symbol = :building)
                    AND (CAST(:floor AS TEXT) IS NULL OR content #>> '{floor,floorId}' = :floor)
                ORDER BY created_at, id
                """
            )
        ).param("building", building)
            .param("floor", floor)
            .query((rows, num) -> this.node(rows))
            .list();
    }

    private JsonNode submission(final UUID id) {
        return this.jdbc.sql(SubmissionStore.SELECT.concat("WHERE id = :id"))
            .param("id", id)
            .query((rows, num) -> this.node(rows))
            .single();
    }

    private JsonNode node(final ResultSet rows) throws SQLException {
        final ObjectNode node = this.mapper.readValue(
            rows.getString("content"), ObjectNode.class
        );
        node.put("id", rows.getString("id"));
        node.put("version", rows.getInt("version"));
        node.put("status", rows.getString("status"));
        node.put(
            "createdAt",
            rows.getObject("created_at", OffsetDateTime.class).toInstant().toString()
        );
        return node;
    }

    /**
     * Wynik zapisu zgłoszenia.
     *
     * @param created Czy zgłoszenie powstało w tym żądaniu
     * @param submission Zgłoszenie w kształcie schematu Submission
     * @since 0.1.0
     */
    record Saved(boolean created, JsonNode submission) {
    }
}
