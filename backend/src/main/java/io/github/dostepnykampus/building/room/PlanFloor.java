/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room;

/**
 * Piętro z planu budynku odczytane z usługi ArcGIS PWr.
 *
 * <p>Wymagania: US16, NFR-40</p>
 *
 * @param id Identyfikator piętra, ten sam co piętro pomieszczenia
 * @param label Etykieta piętra wyliczona z nazwy w danych PWr, np. „2 piętro”
 * @param order Numer piętra; większy to wyżej, 0 to parter
 * @since 0.1.0
 */
public record PlanFloor(String id, String label, int order) {
}
