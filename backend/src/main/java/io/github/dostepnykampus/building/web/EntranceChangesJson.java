/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.api.EntranceChanges;
import io.github.dostepnykampus.building.application.BuildingReading;
import io.github.dostepnykampus.building.domain.MarkedEntrance;
import io.github.dostepnykampus.shared.json.RawJson;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Wejścia z oznaczeniem z obchodu dla innych modułów, zapisane tą samą odpowiedzią co wejścia
 * na karcie budynku, więc mają kształt schematu Entrance kontraktu.
 *
 * <p>Wymagania: US17.2</p>
 *
 * @since 0.1.0
 */
@Component
public final class EntranceChangesJson implements EntranceChanges {

    /** Odczyt budynków. */
    private final BuildingReading reading;

    /** Zapis odpowiedzi w JSON. */
    private final ObjectMapper mapper;

    /**
     * Tworzy odczyt zmian.
     *
     * @param reading Odczyt budynków
     * @param mapper Zapis odpowiedzi w JSON
     */
    public EntranceChangesJson(final BuildingReading reading, final ObjectMapper mapper) {
        this.reading = reading;
        this.mapper = mapper;
    }

    @Override
    public List<RawJson> markedSince(final Instant since) {
        return this.reading.markedSince(since).stream().map(this::json).toList();
    }

    private RawJson json(final MarkedEntrance marked) {
        return new RawJson(
            this.mapper.writeValueAsString(
                new EntranceResponse(marked.building(), marked.entrance())
            )
        );
    }
}
