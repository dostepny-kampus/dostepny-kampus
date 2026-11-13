/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Piętro budynku szczegółowego z danych PWr (`Floor` z kontraktu API).
 *
 * Wymagania: US7.2, US16.1
 *
 * @property id identyfikator piętra
 * @property label etykieta z danych PWr, np. „Piwnica” albo „3 piętro”
 * @property order kolejność w budynku; większa liczba to wyższe piętro
 */
data class Floor(val id: String, val label: String, val order: Int)

/**
 * Piętra budynku ze źródłem danych (`FloorList`). Budynek podstawowy ma pustą listę.
 *
 * Wymagania: BR-G1, NFR-40
 *
 * @property source źródło do podpisu przy liście, „Politechnika Wrocławska”
 * @property floors piętra od najniższego
 */
data class FloorList(val source: String, val floors: List<Floor>)
