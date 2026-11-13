/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.domain.SubmissionFloor;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Podobne zgłoszenia w promieniu 10 m z każdego
 * piętra, od najbliższego, bez odrzuconych, archiwalnych i samego sprawdzanego.
 *
 * <p>Wymagania: US10.1, BR-13</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class SimilarSubmissionsHttpTest {

    @ParameterizedTest(name = "{0} m, {1}")
    @CsvSource({"5, ACCEPTED", "10, ACCEPTED", "9, SENT", "9, IN_REVIEW"})
    void brThirteenShowsCloseOpenSubmission(
        final int meters, final String status, final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie w promieniu 10 m nie jest na liście podobnych",
            new SimilarSubmissionsHttpTest.Place(context).pair(meters, status),
            Matchers.hasSize(1)
        );
    }

    @ParameterizedTest(name = "{0} m, {1}")
    @CsvSource({"11, ACCEPTED", "5, REJECTED", "5, ARCHIVED"})
    void brThirteenHidesFarOrClosedSubmission(
        final int meters, final String status, final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie dalej niż 10 m, odrzucone albo archiwalne jest na liście podobnych",
            new SimilarSubmissionsHttpTest.Place(context).pair(meters, status),
            Matchers.empty()
        );
    }

    @Test
    void usTenOneSortsFromNearestWithDistanceFromAnyFloor(final WebApplicationContext context)
        throws Exception {
        final SimilarSubmissionsHttpTest.Place place =
            new SimilarSubmissionsHttpTest.Place(context);
        final UUID checked = place.add(0, "IN_REVIEW", "A-1-0");
        final UUID far = place.add(5, "ACCEPTED", "A-1-1");
        final UUID near = place.add(2, "SENT", "A-1-0");
        MatcherAssert.assertThat(
            "Podobne zgłoszenia nie są od najbliższego z odległością albo mają sprawdzane",
            place.similar(checked, List.of(checked, far, near)).stream().map(
                item -> "%s %s %s".formatted(
                    item.path("id").asString(), item.path("distanceMeters").asDouble(),
                    item.path("floor").path("floorId").asString()
                )
            ).toList(),
            Matchers.contains("%s 2.0 A-1-0".formatted(near), "%s 5.0 A-1-1".formatted(far))
        );
    }

    @Test
    void skipsCorruptNeighbor(final WebApplicationContext context) throws Exception {
        final SimilarSubmissionsHttpTest.Place place =
            new SimilarSubmissionsHttpTest.Place(context);
        final UUID checked = place.add(0, "IN_REVIEW", "A-1-0");
        final UUID corrupt = place.add(2, "SENT", "A-1-0");
        final UUID valid = place.add(5, "ACCEPTED", "A-1-1");
        context.getBean(JdbcClient.class).sql(
            "UPDATE submissions SET content = content || '{\"photoIds\": \"zepsute\"}' WHERE id = :id"
        ).param("id", corrupt).update();
        MatcherAssert.assertThat(
            "Lista podobnych zgłoszeń ma zawierać tylko czytelne zgłoszenie",
            place.similar(checked, List.of(corrupt, valid)).stream()
                .map(item -> item.path("id").asString()).toList(),
            Matchers.contains(valid.toString())
        );
    }

    @Test
    void usTenOneAnswersNotFoundForMissingSubmission(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Podobne zgłoszenia nieistniejącego zgłoszenia nie dają 404",
            new SubmissionRequests(context)
                .similar(UUID.randomUUID()).getStatus(),
            Matchers.equalTo(404)
        );
    }

    /**
     * Stałe miejsce na kampusie (17,055; 51,108), przy którym test dodaje zgłoszenia.
     *
     * <p>Miejsce leży kilkaset metrów od zgłoszeń, które zapisują na stałe
     * inne klasy testów, więc w promieniu 10 m są tylko zgłoszenia tego testu;
     * każdy test wycofuje je na końcu transakcji.</p>
     *
     * @param context Kontekst aplikacji z bazą i MockMvc
     * @since 0.1.0
     */
    private record Place(WebApplicationContext context) {

        /**
         * Dodaje sprawdzane zgłoszenie i drugie w danej odległości.
         *
         * @param meters Odległość drugiego zgłoszenia w metrach
         * @param status Status drugiego zgłoszenia
         * @return Drugie zgłoszenie, jeśli jest na liście podobnych
         * @throws Exception Gdy MockMvc nie wykona żądania
         */
        List<JsonNode> pair(final int meters, final String status) throws Exception {
            return this.similar(
                this.add(0, "IN_REVIEW", "A-1-0"), List.of(this.add(meters, status, "A-1-0"))
            );
        }

        /**
         * Dodaje zgłoszenie na północ od miejsca.
         *
         * @param meters Odległość od miejsca w metrach
         * @param status Status zgłoszenia
         * @param floor Piętro zgłoszenia
         * @return Identyfikator zgłoszenia
         */
        UUID add(final int meters, final String status, final String floor) {
            final UUID id = new SampleSubmissions(this.context).add(
                status,
                SampleSubmissions.quick(
                    new SubmissionPlace(
                        new SubmissionPoint(17.055, 51.108), null, false, "A-1",
                        new SubmissionFloor(floor, null, false)
                    ),
                    "Próg", UUID.randomUUID()
                )
            );
            this.context.getBean(JdbcClient.class).sql(
                """
                UPDATE submissions SET content = jsonb_set(
                    content, '{location,coordinates}', jsonb_build_array(ST_X(moved), ST_Y(moved))
                ) FROM (
                    SELECT ST_Project(
                        ST_SetSRID(ST_MakePoint(17.055, 51.108), 4326)::geography, :meters, 0
                    )::geometry AS moved
                ) AS place WHERE id = :id
                """
            ).param("meters", meters).param("id", id).update();
            return id;
        }

        /**
         * Pobiera podobne zgłoszenia, tylko spośród dodanych w teście.
         *
         * @param checked Sprawdzane zgłoszenie
         * @param own Zgłoszenia dodane w teście
         * @return Pozycje listy podobnych w kolejności odpowiedzi
         * @throws Exception Gdy MockMvc nie wykona żądania
         */
        List<JsonNode> similar(final UUID checked, final List<UUID> own) throws Exception {
            final MockHttpServletResponse response = new SubmissionRequests(this.context)
                .similar(checked);
            MatcherAssert.assertThat(
                "Podobne zgłoszenia nie dają 200", response.getStatus(), Matchers.equalTo(200)
            );
            return StreamSupport.stream(
                this.context.getBean(ObjectMapper.class)
                    .readTree(response.getContentAsString(StandardCharsets.UTF_8)).spliterator(),
                false
            ).filter(item -> own.contains(UUID.fromString(item.path("id").asString()))).toList();
        }
    }
}
