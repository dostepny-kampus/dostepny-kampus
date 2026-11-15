/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Miejsce zgłoszenia: punkt, dokładność, budynek i piętro.
 *
 * <p>Wymagania: US6, BR-G2, US6.1, US6.3</p>
 *
 * @param point Położenie zgłoszenia
 * @param accuracyMeters Dokładność pozycji w metrach albo null, gdy nie
 *  podano
 * @param manualCorrection Czy użytkownik poprawił pozycję ręcznie
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
     * Miejsce z punktem i dokładnością od 0 do 10000 m; brak dokładności jest
     * dozwolony. Dokładność spoza tego zakresu, nieskończona albo NaN kończy się
     * wyjątkiem InvalidSubmission.
     */
    public SubmissionPlace {
        Objects.requireNonNull(point, "point");
        if (accuracyMeters != null && !(accuracyMeters >= 0 && accuracyMeters <= 10_000)) {
            throw new InvalidSubmission("Dokładność pozycji spoza zakresu 0–10000 m");
        }
    }

    /**
     * Kopia miejsca z innym budynkiem.
     *
     * @param symbol Symbol budynku
     * @return Miejsce z tym budynkiem i pozostałymi polami bez zmian
     */
    public SubmissionPlace withBuilding(final String symbol) {
        return new SubmissionPlace(
            this.point, this.accuracyMeters, this.manualCorrection, symbol, this.floor
        );
    }

    /**
     * Kopia miejsca bez budynku.
     *
     * @return Miejsce bez symbolu budynku, z tym samym piętrem i resztą pól
     */
    public SubmissionPlace outsideBuildings() {
        return new SubmissionPlace(
            this.point, this.accuracyMeters, this.manualCorrection, null, this.floor
        );
    }
}
