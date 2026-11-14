/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.domain;

/**
 * Piętro budynku szczegółowego.
 *
 * <p>Wymagania: US16</p>
 *
 * @param id Identyfikator piętra w obrębie budynku
 * @param label Etykieta piętra z danych PWr
 * @param order Kolejność piętra w budynku; większa liczba to wyższe piętro
 * @since 0.1.0
 */
public record Floor(String id, String label, int order) {
}
