/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.inspection.domain.BuildingProgress;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Postęp jednego budynku w odpowiedzi HTTP.
 *
 * @param buildingSymbol Symbol budynku
 * @param detailLevel Rodzaj danych budynku
 * @param floorsTotal Liczba pięter, tylko w budynku szczegółowym
 * @param floorsInspected Liczba obejrzanych pięter, tylko w budynku szczegółowym
 * @param inspected Czy obejrzano cały budynek, tylko w budynku podstawowym
 * @param pendingSubmissions Liczba zgłoszeń czekających na sprawdzenie
 * @param acceptedSubmissions Liczba przyjętych zgłoszeń
 * @param entrancesTotal Liczba wszystkich wejść
 * @param entrancesCheckedOnSite Liczba wejść sprawdzonych na miejscu
 * @param markers Aktywne znaczniki budynku
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record BuildingProgressResponse(
    String buildingSymbol,
    String detailLevel,
    @Nullable Integer floorsTotal,
    @Nullable Integer floorsInspected,
    @Nullable Boolean inspected,
    int pendingSubmissions,
    int acceptedSubmissions,
    int entrancesTotal,
    int entrancesCheckedOnSite,
    List<ProgressMarkerResponse> markers
) {

    static BuildingProgressResponse from(final BuildingProgress progress) {
        final BuildingProgress.Counts counts = progress.counts();
        final List<ProgressMarkerResponse> markers = progress.markers().stream()
            .map(ProgressMarkerResponse::from).toList();
        return switch (progress.coverage()) {
            case BuildingProgress.Coverage.Floors floors -> new BuildingProgressResponse(
                progress.symbol(), "DETAILED", floors.total(), floors.covered(), null,
                counts.pending(), counts.accepted(), counts.entrances(), counts.checked(), markers
            );
            case BuildingProgress.Coverage.Whole whole -> new BuildingProgressResponse(
                progress.symbol(), "BASIC", null, null, whole.inspected(),
                counts.pending(), counts.accepted(), counts.entrances(), counts.checked(), markers
            );
        };
    }
}
