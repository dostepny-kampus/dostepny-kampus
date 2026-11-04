/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.UUID;
import java.util.regex.Pattern;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdScalarDeserializer;

/**
 * Identyfikator tylko jako napis w postaci 8-4-4-4-12 cyfr szesnastkowych,
 * małymi albo wielkimi literami (OpenAPI {@code format: uuid}, RFC 9562);
 * Base64 i skróty przyjmowane przez {@link UUID#fromString} są błędem danych.
 *
 * @since 0.1.0
 */
final class CanonicalUuidDeserializer extends StdScalarDeserializer<UUID> {

    /**
     * Postać kanoniczna identyfikatora.
     */
    private static final Pattern CANONICAL = Pattern.compile(
        "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"
    );

    CanonicalUuidDeserializer() {
        super(UUID.class);
    }

    @Override
    public UUID deserialize(final JsonParser parser, final DeserializationContext context) {
        final UUID result;
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            result = CanonicalUuidDeserializer.parsed(parser.getString(), context);
        } else {
            result = (UUID) context.handleUnexpectedToken(UUID.class, parser);
        }
        return result;
    }

    private static UUID parsed(final String text, final DeserializationContext context) {
        final UUID result;
        if (CanonicalUuidDeserializer.CANONICAL.matcher(text).matches()) {
            result = UUID.fromString(text);
        } else {
            result = (UUID) context.handleWeirdStringValue(
                UUID.class, text, "Identyfikator spoza postaci 8-4-4-4-12"
            );
        }
        return result;
    }
}
