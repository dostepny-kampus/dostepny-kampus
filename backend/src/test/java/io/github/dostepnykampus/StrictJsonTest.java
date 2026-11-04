/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Testy W2-1–W2-6: wiązanie ciała JSON nie zamienia typów wartości, odrzuca
 * jawny {@code null} i identyfikator spoza postaci 8-4-4-4-12, a poprawne
 * ciało (identyfikator małymi albo wielkimi literami, liczba bez ułamka
 * w polu liczbowym, pole spoza schematu, brak pól nieobowiązkowych) przechodzi.
 *
 * <p>{@code JsonMapper} z tym samym dostosowaniem co w Spring Boot, bez Springa.</p>
 *
 * @since 0.1.0
 */
final class StrictJsonTest {

    /**
     * Identyfikator w postaci kanonicznej, małymi literami.
     */
    private static final String CANONICAL = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    /**
     * Wynik odczytu, po którym serwer odpowie 400.
     */
    private static final String REJECTED = "odrzucone";

    @Test
    @Disabled("Napis wiązany jest jako liczba albo wartość logiczna")
    void strictJsonOneRejectsTextForNumberOrBoolean() {
        MatcherAssert.assertThat(
            "Napis w polu liczbowym albo logicznym przeszedł",
            StrictJsonTest.outcomes(
                "{\"accuracyMeters\":\"5\"}", "{\"manualCorrection\":\"true\"}",
                "{\"version\":\"1\"}"
            ),
            StrictJsonTest.allRejected()
        );
    }

    @Test
    @Disabled("Liczba wiązana jest jako napis")
    void strictJsonTwoRejectsNumberForText() {
        MatcherAssert.assertThat(
            "Liczba w polu tekstowym przeszła",
            StrictJsonTest.outcomes("{\"floorId\":42}"),
            StrictJsonTest.allRejected()
        );
    }

    @Test
    @Disabled("Liczba z ułamkiem wiązana jest jako liczba całkowita")
    void strictJsonThreeRejectsFractionForInteger() {
        MatcherAssert.assertThat(
            "Liczba z ułamkiem w polu całkowitym przeszła",
            StrictJsonTest.outcomes("{\"version\":2.0}", "{\"severity\":1.5}"),
            StrictJsonTest.allRejected()
        );
    }

    @Test
    @Disabled("Jawny null w polu jest przyjmowany jak brak pola")
    void strictJsonFourRejectsExplicitNull() {
        MatcherAssert.assertThat(
            "Jawny null w polu przeszedł",
            StrictJsonTest.outcomes(
                "{\"accuracyMeters\":null}", "{\"manualCorrection\":null}",
                "{\"version\":null}", "{\"floorId\":null}", "{\"id\":null}"
            ),
            StrictJsonTest.allRejected()
        );
    }

    @Test
    @Disabled("Identyfikator w Base64 jest przyjmowany")
    void strictJsonFiveRejectsNonCanonicalUuid() {
        MatcherAssert.assertThat(
            "Identyfikator spoza postaci 8-4-4-4-12 przeszedł",
            StrictJsonTest.outcomes(
                "{\"id\":\"PyUE4E+JEdOaDAMF6CszAQ==\"}", "{\"id\":\"1-1-1-1-1\"}",
                "{\"id\":\"3f2504e04-f89-11d3-9a0c-0305e82c3301\"}",
                "{\"id\":\"3f2504e04f8911d39a0c0305e82c3301\"}"
            ),
            StrictJsonTest.allRejected()
        );
    }

    @Test
    void strictJsonSixKeepsCorrectBodyWithUpperCaseUuid() {
        MatcherAssert.assertThat(
            "Poprawne ciało z polem spoza schematu nie przeszło bez zmian",
            StrictJsonTest.read(
                String.join(
                    "",
                    "{\"accuracyMeters\":5,\"manualCorrection\":false,\"version\":3,",
                    "\"severity\":2,\"floorId\":\"C-13/1\",\"extra\":\"x\",\"id\":\"%s\"}"
                        .formatted(StrictJsonTest.CANONICAL.toUpperCase(Locale.ROOT))
                )
            ),
            Matchers.equalTo(
                new Sample(5.0, false, 3, 2, "C-13/1", UUID.fromString(StrictJsonTest.CANONICAL))
            )
        );
    }

    @Test
    void strictJsonSixLeavesAbsentFieldsEmpty() {
        MatcherAssert.assertThat(
            "Ciało z samym identyfikatorem małymi literami nie przeszło",
            StrictJsonTest.read("{\"id\":\"%s\"}".formatted(StrictJsonTest.CANONICAL)),
            Matchers.equalTo(
                new Sample(null, null, null, null, null, UUID.fromString(StrictJsonTest.CANONICAL))
            )
        );
    }

    private static Matcher<Iterable<? extends String>> allRejected() {
        return Matchers.everyItem(Matchers.endsWith(StrictJsonTest.REJECTED));
    }

    private static List<String> outcomes(final String... bodies) {
        return Stream.of(bodies)
            .map(body -> "%s %s".formatted(body, StrictJsonTest.outcome(body)))
            .toList();
    }

    private static String outcome(final String body) {
        String result;
        try {
            result = "przyjęte jako %s".formatted(StrictJsonTest.read(body));
        } catch (final JacksonException error) {
            result = StrictJsonTest.REJECTED;
        }
        return result;
    }

    private static Sample read(final String body) {
        final JsonMapper.Builder builder = JsonMapper.builder();
        new StrictJsonConfiguration().customize(builder);
        return builder.build().readValue(body, Sample.class);
    }

    /**
     * Ciało żądania z polami rodzajów używanych w zgłoszeniach, każde albo null.
     *
     * @param accuracyMeters Liczba albo null
     * @param manualCorrection Wartość logiczna albo null
     * @param version Liczba całkowita albo null
     * @param severity Liczba całkowita albo null
     * @param floorId Napis albo null
     * @param id Identyfikator albo null
     * @since 0.1.0
     */
    record Sample(
        @Nullable Double accuracyMeters, @Nullable Boolean manualCorrection,
        @Nullable Integer version, @Nullable Integer severity,
        @Nullable String floorId, @Nullable UUID id
    ) {
    }
}
