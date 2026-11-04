/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import java.util.Arrays;

/**
 * Tabela grup barier: wartość schematu BarrierGroup w OpenAPI i krótka stała
 * domeny.
 *
 * @since 0.1.0
 */
enum GroupJson {
    /** Wejście i drzwi. */
    @JsonProperty("ENTRANCE_AND_DOORS")
    ENTRANCE(BarrierGroup.ENTRANCE),
    /** Komunikacja pionowa. */
    @JsonProperty("VERTICAL_CIRCULATION")
    VERTICAL(BarrierGroup.VERTICAL),
    /** Komunikacja pozioma. */
    @JsonProperty("HORIZONTAL_CIRCULATION")
    HORIZONTAL(BarrierGroup.HORIZONTAL),
    /** Toalety. */
    @JsonProperty("TOILETS")
    TOILETS(BarrierGroup.TOILETS),
    /** Orientacja i informacja. */
    @JsonProperty("ORIENTATION_AND_INFORMATION")
    ORIENTATION(BarrierGroup.ORIENTATION),
    /** Inne. */
    @JsonProperty("OTHER")
    OTHER(BarrierGroup.OTHER);
    /** Stała domeny tej wartości. */
    private final BarrierGroup domain;

    GroupJson(final BarrierGroup group) {
        this.domain = group;
    }

    /**
     * Wartość JSON dla stałej domeny.
     *
     * @param group Grupa barier w domenie
     * @return Wartość z tabeli
     */
    static GroupJson of(final BarrierGroup group) {
        return Arrays.stream(GroupJson.values()).filter(json -> json.domain == group)
            .findFirst().orElseThrow();
    }

    /**
     * Stała domeny.
     *
     * @return Grupa barier w domenie
     */
    BarrierGroup group() {
        return this.domain;
    }
}
