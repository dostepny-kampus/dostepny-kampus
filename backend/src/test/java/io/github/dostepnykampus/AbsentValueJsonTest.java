/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import io.github.dostepnykampus.shared.json.StrictJsonConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

/**
 * Wiązanie ścisłe JSON nie zmienia wartości brakującego pola: pole bez wartości
 * dostaje wartość braku z deserializatora jego typu.
 *
 * @since 0.1.0
 */
final class AbsentValueJsonTest {

    @Test
    void absentFieldTakesValueFromTypeDeserializer() {
        final JsonMapper.Builder builder = JsonMapper.builder().addModule(
            new SimpleModule().addDeserializer(Mark.class, new AbsentValueJsonTest.Marks())
        );
        new StrictJsonConfiguration().customize(builder);
        Assertions.assertEquals(
            new Holder(new Mark("brak")),
            builder.build().readValue("{}", Holder.class)
        );
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
