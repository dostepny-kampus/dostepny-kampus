/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
 * @param counts Liczby zgłoszeń i wejść budynku
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
    @JsonUnwrapped Counts counts,
    List<ProgressMarkerResponse> markers
) {

    static BuildingProgressResponse from(final BuildingProgress progress) {
        final BuildingProgress.Counts source = progress.counts();
        final Counts counts = new Counts(
            source.pending(), source.accepted(), source.entrances(), source.checked()
        );
        final List<ProgressMarkerResponse> markers = progress.markers().stream()
            .map(ProgressMarkerResponse::from).toList();
        return switch (progress.coverage()) {
            case BuildingProgress.Coverage.Floors floors -> new BuildingProgressResponse(
                progress.symbol(), "DETAILED", floors.total(), floors.covered(), null,
                counts, markers
            );
            case BuildingProgress.Coverage.Whole whole -> new BuildingProgressResponse(
                progress.symbol(), "BASIC", null, null, whole.inspected(),
                counts, markers
            );
            case null -> throw new IllegalArgumentException("Brak pokrycia budynku");
        };
    }

    /**
     * Liczby zgłoszeń i wejść budynku w odpowiedzi HTTP.
     *
     * @param pending Zgłoszenia czekające
     * @param accepted Zgłoszenia przyjęte
     * @param entrances Wszystkie wejścia
     * @param checked Wejścia sprawdzone na miejscu
     */
    record Counts(
        @JsonProperty("pendingSubmissions") int pending,
        @JsonProperty("acceptedSubmissions") int accepted,
        @JsonProperty("entrancesTotal") int entrances,
        @JsonProperty("entrancesCheckedOnSite") int checked
    ) {
    }
}
