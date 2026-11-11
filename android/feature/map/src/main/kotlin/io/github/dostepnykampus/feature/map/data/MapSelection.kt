/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

/**
 * Zaznaczenie na mapie V1. Zmienia się przy każdym wyborze osoby, a dane katalogu [CampusMapData] zostają te same,
 * więc mapa odróżnia „zmienił się katalog” od „zmieniło się zaznaczenie”.
 *
 * @property building symbol budynku z grubszym obrysem, np. po „Wstecz” z V2 (US1.1); `null` bez zaznaczenia
 * @property entrance wejście z pinezką i podpisem, np. po „Pokaż wejście na mapie” (US3.3, decyzja 134); `null`
 *   bez zaznaczenia
 * @property bounds teren pokazany na mapie: teren zaznaczonego budynku, a bez niego teren startowy z
 *   [CampusMapData.bounds] (US1.8, BR-G5); `null` bez budynków
 */
data class MapSelection(
    val building: String? = null,
    val entrance: SelectedEntrance? = null,
    val bounds: CampusBounds? = null,
)

/**
 * Zaznaczenie budynku [building] i wejścia [entrance] na terenie budynku, a bez niego budynku wejścia. Budynek
 * z innego terenu przenosi na swój teren granicę mapy (US1.8, BR-G5).
 */
fun CampusMapData.selection(building: String? = null, entrance: SelectedEntrance? = null): MapSelection =
    MapSelection(building = building, entrance = entrance, bounds = boundsOf(building ?: entrance?.buildingSymbol))
