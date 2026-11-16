/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;

/**
 * Owija deserializator napisów w {@link NulRejectingDeserializer}; deserializatory innych
 * typów zostawia bez zmian.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class NulRejectingModifier extends ValueDeserializerModifier {

    /**
     * Wersja postaci serializowanej.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public ValueDeserializer<?> modifyDeserializer(
        final DeserializationConfig config, final BeanDescription.Supplier description,
        final ValueDeserializer<?> deserializer
    ) {
        ValueDeserializer<?> result = deserializer;
        if (String.class.equals(description.getBeanClass())) {
            result = new NulRejectingDeserializer(deserializer);
        }
        return result;
    }
}
