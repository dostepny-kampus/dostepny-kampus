/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import io.github.dostepnykampus.shared.json.StrictJsonConfiguration;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Testy W2-1–W2-6: wiązanie ciała JSON nie zamienia typów wartości, odrzuca
 * jawny {@code null} i identyfikator spoza postaci 8-4-4-4-12, a poprawne
 * ciało (identyfikator małymi albo wielkimi literami, liczba bez ułamka
 * w polu liczbowym, pole spoza schematu, brak pól nieobowiązkowych) przechodzi.
 * Brakujące pole dostaje wartość braku z deserializatora swojego typu.
 *
 * <p>{@code JsonMapper} z tym samym dostosowaniem co w Spring Boot, bez Springa.</p>
 *
 * @since 0.1.0
 */
final class StrictJsonConfigurationTest {

    /**
     * Identyfikator w postaci kanonicznej, małymi literami.
     */
    private static final String CANONICAL = "3f2504e0-4f89-11d3-9a0c-0305e82c3301";

    /**
     * Ten sam identyfikator wielkimi literami.
     */
    private static final String UPPER =
        StrictJsonConfigurationTest.CANONICAL.toUpperCase(Locale.ROOT);

    /**
     * Wynik odczytu odrzuconego jako błąd danych, po którym serwer odpowie 400.
     */
    private static final String REJECTED = "odrzucone";

    @Test
    void rejectsTextForNumberOrBoolean() {
        MatcherAssert.assertThat(
            "Napis w polu liczbowym albo logicznym przeszedł",
            StrictJsonConfigurationTest.outcomes(
                "{\"accuracyMeters\":\"5\"}", "{\"manualCorrection\":\"true\"}",
                "{\"version\":\"1\"}"
            ),
            StrictJsonConfigurationTest.allRejected()
        );
    }

    @Test
    void rejectsNumberForText() {
        MatcherAssert.assertThat(
            "Liczba w polu tekstowym przeszła",
            StrictJsonConfigurationTest.outcomes("{\"floorId\":42}"),
            StrictJsonConfigurationTest.allRejected()
        );
    }

    @Test
    void rejectsFractionForInteger() {
        MatcherAssert.assertThat(
            "Liczba z ułamkiem w polu całkowitym przeszła",
            StrictJsonConfigurationTest.outcomes("{\"version\":2.0}", "{\"severity\":1.5}"),
            StrictJsonConfigurationTest.allRejected()
        );
    }

    @Test
    void rejectsExplicitNull() {
        MatcherAssert.assertThat(
            "Jawny null w polu przeszedł",
            StrictJsonConfigurationTest.outcomes(
                "{\"accuracyMeters\":null}", "{\"manualCorrection\":null}",
                "{\"version\":null}", "{\"floorId\":null}", "{\"id\":null}"
            ),
            StrictJsonConfigurationTest.allRejected()
        );
    }

    @Test
    void rejectsNonCanonicalUuid() {
        MatcherAssert.assertThat(
            "Identyfikator spoza postaci 8-4-4-4-12 przeszedł",
            StrictJsonConfigurationTest.outcomes(
                "{\"id\":\"PyUE4E+JEdOaDAMF6CszAQ==\"}", "{\"id\":\"1-1-1-1-1\"}",
                "{\"id\":\"3f2504e04-f89-11d3-9a0c-0305e82c3301\"}",
                "{\"id\":\"3f2504e04f8911d39a0c0305e82c3301\"}"
            ),
            StrictJsonConfigurationTest.allRejected()
        );
    }

    @Test
    void keepsCorrectBodiesWithUuidInBothCases() {
        final UUID id = UUID.fromString(StrictJsonConfigurationTest.CANONICAL);
        MatcherAssert.assertThat(
            "Poprawne ciało z polem spoza schematu albo bez pól nieobowiązkowych nie przeszło",
            List.of(
                StrictJsonConfigurationTest.read(
                    String.join(
                        "",
                        "{\"accuracyMeters\":5,\"manualCorrection\":false,\"version\":3,",
                        "\"severity\":2,\"floorId\":\"C-13/1\",\"extra\":\"x\",\"id\":\"%s\"}"
                            .formatted(StrictJsonConfigurationTest.UPPER)
                    )
                ),
                StrictJsonConfigurationTest.read(
                    "{\"id\":\"%s\"}".formatted(StrictJsonConfigurationTest.CANONICAL)
                )
            ),
            Matchers.contains(
                new Sample(5.0, false, 3, 2, "C-13/1", id),
                new Sample(null, null, null, null, null, id)
            )
        );
    }

    @Test
    void takesAbsentFieldValueFromTypeDeserializer() {
        final JsonMapper.Builder builder = JsonMapper.builder().addModule(
            new SimpleModule().addDeserializer(Mark.class, new StrictJsonConfigurationTest.Marks())
        );
        new StrictJsonConfiguration().customize(builder);
        Assertions.assertEquals(
            new Holder(new Mark("brak")),
            builder.build().readValue("{}", Holder.class),
            "Brakujące pole przyjmuje wartość z deserializatora swojego typu"
        );
    }

    private static Matcher<Iterable<? extends String>> allRejected() {
        return Matchers.everyItem(Matchers.endsWith(StrictJsonConfigurationTest.REJECTED));
    }

    private static List<String> outcomes(final String... bodies) {
        return Stream.of(bodies)
            .map(body -> "%s %s".formatted(body, StrictJsonConfigurationTest.outcome(body)))
            .toList();
    }

    private static String outcome(final String body) {
        String result;
        try {
            result = "przyjęte jako %s".formatted(StrictJsonConfigurationTest.read(body));
        } catch (final MismatchedInputException error) {
            result = StrictJsonConfigurationTest.REJECTED;
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

    /**
     * Wartość z własnym deserializatorem.
     *
     * @param text Treść
     * @since 0.1.0
     */
    record Mark(String text) {
    }

    /**
     * Obiekt z jednym polem typu {@link Mark}.
     *
     * @param mark Pole
     * @since 0.1.0
     */
    record Holder(Mark mark) {
    }

    /**
     * Deserializator {@link Mark}: tekst z JSON albo „brak” dla brakującego pola.
     *
     * @since 0.1.0
     */
    private static final class Marks extends StdDeserializer<Mark> {

        Marks() {
            super(Mark.class);
        }

        @Override
        public Mark deserialize(final JsonParser parser, final DeserializationContext context) {
            return new Mark(parser.getString());
        }

        @Override
        public Mark getAbsentValue(final DeserializationContext context) {
            return new Mark("brak");
        }
    }
}
