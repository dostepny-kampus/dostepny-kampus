/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import tools.jackson.databind.json.JsonMapper;

/**
 * Mapper JSON z tym samym ścisłym dostosowaniem co serwer, bez Springa.
 *
 * @since 0.1.0
 */
public final class StrictMapper {

    /**
     * Buduje mapper.
     *
     * @return Mapper ze ścisłym wiązaniem
     */
    public JsonMapper build() {
        final JsonMapper.Builder builder = JsonMapper.builder();
        new StrictJsonConfiguration().customize(builder);
        return builder.build();
    }
}
