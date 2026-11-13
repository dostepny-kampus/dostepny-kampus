/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.MutableCoercionConfig;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.LogicalType;

/**
 * Konfiguracja ścisłego wiązania ciał JSON żądań w całym serwerze; nieznane pola są pomijane.
 * Wartość innego typu niż pole (napis zamiast liczby albo wartości logicznej, liczba zamiast
 * napisu, ułamek w polu całkowitym, pusty napis), jawny {@code null} i identyfikator spoza
 * postaci 8-4-4-4-12 dają błąd danych, który Spring zamienia na 400; brak pola daje
 * {@code null}.
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
public final class StrictJsonConfiguration implements JsonMapperBuilderCustomizer {

    /**
     * Tworzy konfigurację bez stanu.
     */
    public StrictJsonConfiguration() {
        // Bez stanu: reguły wiązania są stałe.
    }

    @Override
    public void customize(final JsonMapper.Builder builder) {
        builder.disable(
            DeserializationFeature.ACCEPT_FLOAT_AS_INT,
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        ).withCoercionConfigDefaults(
            StrictJsonConfiguration.failing(CoercionInputShape.EmptyString)
        ).withCoercionConfig(
            LogicalType.Integer,
            StrictJsonConfiguration.failing(CoercionInputShape.String, CoercionInputShape.Float)
        ).withCoercionConfig(
            LogicalType.Float, StrictJsonConfiguration.failing(CoercionInputShape.String)
        ).withCoercionConfig(
            LogicalType.Boolean,
            StrictJsonConfiguration.failing(CoercionInputShape.String, CoercionInputShape.Integer)
        ).withCoercionConfig(
            LogicalType.Enum, StrictJsonConfiguration.failing(CoercionInputShape.Integer)
        ).withCoercionConfig(
            LogicalType.Textual,
            StrictJsonConfiguration.failing(
                CoercionInputShape.Integer, CoercionInputShape.Float, CoercionInputShape.Boolean
            )
        ).addModule(
            new SimpleModule("strict-json")
                .setDeserializerModifier(new NullRejectingModifier())
                .addDeserializer(UUID.class, new CanonicalUuidDeserializer())
        );
    }

    private static Consumer<MutableCoercionConfig> failing(final CoercionInputShape... shapes) {
        return config -> {
            for (final CoercionInputShape shape : shapes) {
                config.setCoercion(shape, CoercionAction.Fail);
            }
        };
    }
}
