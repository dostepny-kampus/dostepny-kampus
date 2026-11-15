/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.campus

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.feature.map.area.areaOf
import io.github.dostepnykampus.feature.map.plan.ShownPlan

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
 * @property barrier identyfikator bariery z otwartym arkuszem; jej znak jest na mapie większy i w obwódce;
 * `null` bez wybranej bariery
 * @property boundary wielokąt granicy terenu pokazanego na mapie; `null`, gdy granicą jest prostokąt [bounds]
 * @property plan plan piętra budynku przy środku mapy; `null`, gdy planu nie widać
 */
data class MapSelection(
    val building: String? = null,
    val entrance: SelectedEntrance? = null,
    val bounds: CampusBounds? = null,
    val barrier: String? = null,
    val boundary: GeoArea? = null,
    val plan: ShownPlan? = null,
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
