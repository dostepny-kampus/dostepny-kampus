/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Postęp inwentaryzacji jednego budynku z liczbami pięter, zgłoszeń, wejść i aktywnych znaczników.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-21, BR-24</p>
 *
 * @param symbol Symbol budynku
 * @param detail Poziom szczegółowości: DETAILED albo BASIC
 * @param floors Liczba pięter z danych PWr albo null dla BASIC
 * @param covered Liczba pięter obejrzanych albo null dla BASIC
 * @param inspected Znacznik obejrzanego budynku albo null dla DETAILED
 * @param pending Zgłoszenia wysłane i w weryfikacji
 * @param accepted Zgłoszenia przyjęte, bez archiwalnych
 * @param entrances Wszystkie wejścia budynku
 * @param checked Wejścia sprawdzone na miejscu
 * @param markers Aktywne znaczniki do cofnięcia
 * @since 0.1.0
 */
public record BuildingProgress(
    String symbol,
    String detail,
    @Nullable Integer floors,
    @Nullable Integer covered,
    @Nullable Boolean inspected,
    int pending,
    int accepted,
    int entrances,
    int checked,
    List<ProgressMarker> markers
) {

    /**
     * Utrwala niemodyfikowalną listę znaczników.
     */
    public BuildingProgress {
        markers = List.copyOf(markers);
    }
}
