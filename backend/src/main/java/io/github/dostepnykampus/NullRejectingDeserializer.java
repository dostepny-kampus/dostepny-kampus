/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.util.AccessPattern;

/**
 * Deserializator, który wiąże wartość jak opakowany, ale jawny {@code null}
 * kończy błędem danych; brak pola daje wartość braku opakowanego
 * deserializatora. Jackson nie odróżnia tych przypadków ustawieniem
 * {@code Nulls.FAIL} przy konstruktorze rekordu.
 *
 * @since 0.1.0
 */
final class NullRejectingDeserializer extends DelegatingDeserializer {

    NullRejectingDeserializer(final ValueDeserializer<?> origin) {
        super(origin);
    }

    @Override
    public Object getNullValue(final DeserializationContext context) {
        return context.reportInputMismatch(this, "Jawny null w polu jest niedozwolony");
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        return AccessPattern.DYNAMIC;
    }

    @Override
    public @Nullable Object getAbsentValue(final DeserializationContext context) {
        return this.getDelegatee().getAbsentValue(context);
    }

    @Override
    public ValueDeserializer<?> newDelegatingInstance(final ValueDeserializer<?> delegatee) {
        return new NullRejectingDeserializer(delegatee);
    }
}
