/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Piętro budynku szczegółowego z danych PWr (`Floor` z kontraktu API, US7.2, US16.1).
 *
 * @property id identyfikator piętra
 * @property label etykieta z danych PWr, np. „Piwnica” albo „3 piętro”
 * @property order kolejność w budynku; większa liczba to wyższe piętro
 */
data class Floor(val id: String, val label: String, val order: Int)

/**
 * Piętra budynku ze źródłem danych (`FloorList`). Budynek podstawowy ma pustą listę (BR-G1).
 *
 * @property source źródło do podpisu przy liście, „Politechnika Wrocławska” (NFR-40)
 * @property floors piętra od najniższego
 */
data class FloorList(val source: String, val floors: List<Floor>)
