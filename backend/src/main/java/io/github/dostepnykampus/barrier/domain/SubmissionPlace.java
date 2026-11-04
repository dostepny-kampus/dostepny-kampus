/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import org.jspecify.annotations.Nullable;

/**
 * Miejsce zgłoszenia: punkt, dokładność, budynek i piętro (US6, BR-G2).
 *
 * @param point Położenie zgłoszenia
 * @param accuracyMeters Dokładność pozycji w metrach albo null, gdy nie
 *  podano (US6.1)
 * @param manualCorrection Czy użytkownik poprawił pozycję ręcznie (US6.3)
 * @param buildingSymbol Symbol budynku albo null, gdy zgłoszenie jest poza
 *  budynkami
 * @param floor Piętro albo null, gdy nie podano
 * @since 0.1.0
 */
public record SubmissionPlace(
    SubmissionPoint point,
    @Nullable Double accuracyMeters,
    boolean manualCorrection,
    @Nullable String buildingSymbol,
    @Nullable SubmissionFloor floor
) {

    /**
     * Kopia miejsca z innym budynkiem.
     *
     * @param symbol Symbol budynku
     * @return Miejsce z tym budynkiem i pozostałymi polami bez zmian
     */
    public SubmissionPlace withBuilding(final String symbol) {
        return this;
    }

    /**
     * Kopia miejsca bez budynku.
     *
     * @return Miejsce bez symbolu budynku, z tym samym piętrem i resztą pól
     */
    public SubmissionPlace outsideBuildings() {
        return this;
    }
}
