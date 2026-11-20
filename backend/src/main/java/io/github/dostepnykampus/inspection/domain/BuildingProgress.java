/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.util.List;

/**
 * Postęp inwentaryzacji jednego budynku z pokryciem pięter, liczbami zgłoszeń i wejść oraz
 * aktywnymi znacznikami.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-21, BR-24</p>
 *
 * @param symbol Symbol budynku
 * @param coverage Pokrycie pięter (szczegółowy) albo całego budynku (podstawowy)
 * @param counts Liczby zgłoszeń i wejść budynku
 * @param markers Aktywne znaczniki do cofnięcia
 * @since 0.1.0
 */
public record BuildingProgress(
    String symbol,
    Coverage coverage,
    Counts counts,
    List<ProgressMarker> markers
) {

    /**
     * Utrwala niemodyfikowalną listę znaczników.
     */
    public BuildingProgress {
        markers = List.copyOf(markers);
    }

    /**
     * Sposób liczenia pokrycia budynku: piętra albo cały budynek.
     *
     * @since 0.1.0
     */
    public sealed interface Coverage permits Coverage.Floors, Coverage.Whole {

        /**
         * Budynek szczegółowy z piętrami z danych PWr.
         *
         * @param total Wszystkie piętra budynku
         * @param covered Piętra z aktywnym znacznikiem
         * @since 0.1.0
         */
        record Floors(int total, int covered) implements Coverage {
        }

        /**
         * Budynek podstawowy z jednym znacznikiem całego budynku.
         *
         * @param inspected Prawda, gdy istnieje aktywny znacznik budynku
         * @since 0.1.0
         */
        record Whole(boolean inspected) implements Coverage {
        }
    }

    /**
     * Liczby zgłoszeń i wejść budynku w postępie inwentaryzacji.
     *
     * @param pending Zgłoszenia wysłane i w weryfikacji
     * @param accepted Zgłoszenia przyjęte, bez archiwalnych
     * @param entrances Wszystkie wejścia budynku
     * @param checked Wejścia sprawdzone na miejscu
     * @since 0.1.0
     */
    public record Counts(int pending, int accepted, int entrances, int checked) {
    }
}
