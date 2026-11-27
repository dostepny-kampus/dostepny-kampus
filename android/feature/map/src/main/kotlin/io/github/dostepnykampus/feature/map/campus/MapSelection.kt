/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoArea

/**
 * Zaznaczenie na mapie V1. Zmienia się przy każdym wyborze osoby, a dane katalogu [CampusMapData] zostają te same,
 * więc mapa odróżnia „zmienił się katalog” od „zmieniło się zaznaczenie”.
 *
 * Wymagania: US1.1, US3.3, US1.7, US1.8, BR-G5, US1.5, NFR-21, US16.1
 *
 * @property building symbol budynku z grubszym obrysem, np. po „Wstecz” z V2; `null` bez zaznaczenia
 * @property entrance wejście z pinezką i podpisem, np. po „Pokaż wejście na mapie”; `null`
 *   bez zaznaczenia
 * @property bounds teren pokazany na mapie: teren wybrany przez osobę, bez wyboru teren zaznaczonego
 *   budynku, a bez niego teren startowy z [CampusMapData.bounds]; `null` bez budynków
 * @property barrier identyfikator bariery z otwartym arkuszem, albo `null`
 * @property barrierCaption podpis zaznaczonej bariery, albo `null` bez podpisu
 * @property boundary wielokąt granicy terenu pokazanego na mapie; `null`, gdy granicą jest prostokąt [bounds]
 * @property plan plan piętra budynku przy środku mapy; `null`, gdy planu nie widać
 * @property roomSteps numery kroków na widocznym piętrze
 */
data class MapSelection(
    val building: String? = null,
    val entrance: SelectedEntrance? = null,
    val bounds: CampusBounds? = null,
    val barrier: String? = null,
    val boundary: GeoArea? = null,
    val plan: ShownPlan? = null,
    val roomSteps: List<RoomStepMarker> = emptyList(),
    val barrierCaption: String? = null,
)

/**
 * Zaznaczenie budynku [building] i wejścia [entrance] na terenie o nazwie [area] wybranym przez osobę,
 * a bez wyboru na terenie budynku albo budynku wejścia. Budynek z innego terenu przenosi na swój teren granicę
 * mapy.
 *
 * Wymagania: US1.7, US1.8, BR-G5
 */
fun CampusMapData.selection(
    building: String? = null,
    entrance: SelectedEntrance? = null,
    area: String? = null,
): MapSelection {
    val shown = areaOf(chosen = area, symbol = building ?: entrance?.buildingSymbol)
    return MapSelection(building = building, entrance = entrance, bounds = shown?.bounds, boundary = shown?.boundary)
}

/**
 * Teren pokazany na mapie: teren o nazwie [chosen] wybrany przez osobę, bez wyboru teren budynku [symbol],
 * np. z wyszukiwania, a bez niego teren startowy. `null` bez budynków w katalogu.
 *
 * Wymagania: US1.7, US1.8, US1.6
 */
fun CampusMapData.areaOf(chosen: String?, symbol: String?): MapArea? =
    areas.firstOrNull { it.name == chosen } ?: areas.firstOrNull { it.bounds == boundsOf(symbol) }
