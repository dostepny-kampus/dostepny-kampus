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
import org.springframework.http.ProblemDetail;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
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
        return new Saved(
            this.write(
                """
                INSERT INTO submissions (id, kind, building_symbol, location, content)
                VALUES (:id, :kind, :building,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), CAST(:content AS JSONB))
                ON CONFLICT (id) DO NOTHING
                """,
                draft,
                1
            ) == 1,
            this.find(draft.id()).orElseThrow()
        );
    }

    /**
     * Zapisuje edycję zaczętą od wersji z telefonu i podnosi wersję o 1.
     * Zgłoszenie odrzucone wraca do statusu „wysłane” (US8.3, BR-3).
     * Inna wersja na serwerze albo zgłoszenie przyjęte lub archiwalne
     * daje konflikt z wersją z serwera i niczego nie zmienia (BR-10, BR-18).
     *
     * @param draft Sprawdzona treść edycji
     * @param version Wersja, od której zaczęto edycję w telefonie
     * @return Zgłoszenie po edycji
     */
    JsonNode update(final SubmissionDraft draft, final int version) {
        final int changed = this.write(
            """
            UPDATE submissions SET version = version + 1,
                status = CASE status WHEN 'REJECTED' THEN 'SENT' ELSE status END,
                kind = :kind, building_symbol = :building,
                location = ST_SetSRID(ST_MakePoint(:lon, :lat), 4326),
                content = CAST(:content AS JSONB)
            WHERE id = :id AND version = :version
                AND status IN ('SENT', 'IN_REVIEW', 'REJECTED')
            """,
            draft,
            version
        );
        final JsonNode server = this.find(draft.id()).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND)
        );
        if (changed == 0) {
            final ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Na serwerze jest inna wersja zgłoszenia albo jego status nie pozwala na zmianę"
            );
            problem.setProperty("serverSubmission", server);
            throw new ErrorResponseException(HttpStatus.CONFLICT, problem, null);
        }
        return server;
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

    private Optional<JsonNode> find(final UUID id) {
        return this.jdbc.sql(SubmissionStore.SELECT.concat("WHERE id = :id"))
            .param("id", id)
            .query((rows, num) -> this.node(rows))
            .optional();
    }

    private int write(final String sql, final SubmissionDraft draft, final int version) {
        final ObjectNode content = draft.content();
        if ("QUICK".equals(draft.kind())) {
            content.remove("buildingSymbol");
            this.outlines.containing(draft.lat(), draft.lon())
                .ifPresent(symbol -> content.put("buildingSymbol", symbol));
        }
        try {
            return this.jdbc.sql(sql)
                .param("id", draft.id())
                .param("version", version)
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
