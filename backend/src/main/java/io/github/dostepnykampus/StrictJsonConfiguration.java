/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Ścisłe wiązanie ciał JSON żądań w całym serwerze; nieznane pola są
 * pomijane, jak w domyślnym ustawieniu Spring Boot.
 *
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
public final class StrictJsonConfiguration implements JsonMapperBuilderCustomizer {

    @Override
    public void customize(final JsonMapper.Builder builder) {
        builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
