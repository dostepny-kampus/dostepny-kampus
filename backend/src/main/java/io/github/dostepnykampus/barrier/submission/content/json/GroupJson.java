/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content.json;

import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import java.util.Arrays;

/**
 * Tabela grup barier: wartość schematu w OpenAPI i krótka stała domeny.
 *
 * @since 0.1.0
 */
public enum GroupJson {

    /** Wejście i drzwi. */
    ENTRANCE_AND_DOORS(BarrierGroup.ENTRANCE),
    /** Komunikacja pionowa. */
    VERTICAL_CIRCULATION(BarrierGroup.VERTICAL),
    /** Komunikacja pozioma. */
    HORIZONTAL_CIRCULATION(BarrierGroup.HORIZONTAL),
    /** Toalety. */
    TOILETS(BarrierGroup.TOILETS),
    /** Orientacja i informacja. */
    ORIENTATION_AND_INFORMATION(BarrierGroup.ORIENTATION),
    /** Inne. */
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
    public static GroupJson of(final BarrierGroup group) {
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
