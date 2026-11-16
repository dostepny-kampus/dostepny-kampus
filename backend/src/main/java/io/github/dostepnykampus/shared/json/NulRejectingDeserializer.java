/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;

/**
 * Napis z JSON bez znaku NUL; napis ze znakiem NUL daje błąd danych, który Spring zamienia
 * na 400. Pozostałe zachowanie, w tym odczyt braku i {@code null}, przejmuje od
 * deserializatora, który owija.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class NulRejectingDeserializer extends DelegatingDeserializer {

    NulRejectingDeserializer(final ValueDeserializer<?> origin) {
        super(origin);
    }

    @Override
    public Object deserialize(final JsonParser parser, final DeserializationContext context) {
        final Object value = super.deserialize(parser, context);
        if (value instanceof String text && text.indexOf(0) >= 0) {
            context.reportInputMismatch(this, "Znak NUL w napisie jest niedozwolony");
        }
        return value;
    }

    @Override
    public ValueDeserializer<?> newDelegatingInstance(final ValueDeserializer<?> delegatee) {
        return new NulRejectingDeserializer(delegatee);
    }
}
