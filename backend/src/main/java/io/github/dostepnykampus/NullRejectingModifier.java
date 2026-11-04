/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.type.CollectionType;

/**
 * Opakowuje deserializatory wartości, wyliczeń i kolekcji w
 * {@link NullRejectingDeserializer}, żeby jawny {@code null} w polu był błędem danych.
 *
 * @since 0.1.0
 */
final class NullRejectingModifier extends ValueDeserializerModifier {

    /**
     * Wersja postaci serializowanej klasy bazowej.
     */
    private static final long serialVersionUID = 1L;

    @Override
    public ValueDeserializer<?> modifyDeserializer(
        final DeserializationConfig config, final BeanDescription.Supplier description,
        final ValueDeserializer<?> deserializer
    ) {
        return new NullRejectingDeserializer(deserializer);
    }

    @Override
    public ValueDeserializer<?> modifyEnumDeserializer(
        final DeserializationConfig config, final JavaType type,
        final BeanDescription.Supplier description, final ValueDeserializer<?> deserializer
    ) {
        return new NullRejectingDeserializer(deserializer);
    }

    @Override
    public ValueDeserializer<?> modifyCollectionDeserializer(
        final DeserializationConfig config, final CollectionType type,
        final BeanDescription.Supplier description, final ValueDeserializer<?> deserializer
    ) {
        return new NullRejectingDeserializer(deserializer);
    }
}
