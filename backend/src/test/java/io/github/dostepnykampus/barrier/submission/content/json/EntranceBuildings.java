/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content.json;

import io.github.dostepnykampus.building.api.EntrancePlace;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Budynki z jednym wejściem node/7 w podanym miejscu.
 *
 * @since 0.1.0
 */
final class EntranceBuildings implements KnownBuildings {

    /**
     * Miejsce wejścia node/7.
     */
    private final EntrancePlace place;

    /**
     * Tworzy budynki z wejściem.
     *
     * @param place Miejsce wejścia node/7
     */
    EntranceBuildings(final EntrancePlace place) {
        this.place = place;
    }

    @Override
    public boolean known(final String symbol) {
        return true;
    }

    @Override
    public Optional<EntrancePlace> entrance(final String id, final @Nullable String symbol) {
        return Optional.of(this.place).filter(found -> "node/7".equals(id));
    }
}
