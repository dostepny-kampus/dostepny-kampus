/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.AreaChoice
import io.github.dostepnykampus.feature.map.data.AreaNeighbor
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapArea
import io.github.dostepnykampus.feature.map.data.areaChoices
import io.github.dostepnykampus.feature.map.data.areaNeighbor
import io.github.dostepnykampus.feature.map.data.areaOf
import io.github.dostepnykampus.feature.map.data.center
import io.github.dostepnykampus.feature.map.navigation.MapFocus

/**
 * Tereny mapy V1: który teren mapa pokazuje i na jakie można go zmienić (US1.6, US1.7).
 *
 * @param data dane mapy z terenami; `null`, dopóki ich nie ma
 * @param symbol budynek zaznaczenia z trasy; bez wyboru osoby to on wyznacza teren (US1.8)
 * @param chosen zapisywana nazwa terenu wybranego przez osobę; `null` bez wyboru
 */
internal class MapAreas(
    private val data: CampusMapData?,
    private val symbol: String?,
    private val chosen: MutableState<String?>,
) {
    /** Nazwa terenu wybranego przez osobę; `null`, gdy teren wynika z zaznaczenia albo jest startowy. */
    val chosenName: String? get() = chosen.value

    /** Teren pokazany na mapie; `null` bez danych mapy. */
    val current: MapArea? get() = data?.areaOf(chosen.value, symbol)

    /** Lista terenów: pokazany pierwszy, pozostałe od najbliższego, z liczbą budynków i odległością (US1.7). */
    val choices: List<AreaChoice> get() = current?.let { data?.areaChoices(it) }.orEmpty()

    /** Sąsiedni teren dla środka widoku [view] na granicy pokazanego terenu; `null` z dala od granicy (US1.7). */
    fun neighbor(view: GeoPoint): AreaNeighbor? = current?.let { data?.areaNeighbor(it, view) }

    /**
     * Wybiera teren [name] i zwraca jego widok startowy: środek terenu w powiększeniu startowym (US1.7, BR-G5).
     * `null` i brak zmiany, gdy dane nie mają takiego terenu.
     */
    fun show(name: String): MapCamera? =
        data?.areas?.firstOrNull { it.name == name }?.let { area ->
            chosen.value = name
            MapCamera(center = area.bounds.center, zoom = START_ZOOM)
        }
}

/**
 * Tereny mapy z danych [layers] i zaznaczenia [focus] z trasy. Wybór osoby jest zapisywany, więc wraca po obrocie
 * ekranu i po „Wstecz” z V2 (NFR-22); nowe zaznaczenie z trasy, np. budynek z wyszukiwania, znów wyznacza teren
 * (US1.8).
 */
@Composable
internal fun rememberMapAreas(layers: CampusMapData?, focus: MapFocus?): MapAreas {
    val chosen = rememberSaveable(focus) { mutableStateOf<String?>(null) }
    val symbol =
        when (focus) {
            is MapFocus.Building -> focus.symbol
            is MapFocus.Entrance -> focus.buildingSymbol
            null -> null
        }
    return remember(layers, symbol, chosen) { MapAreas(data = layers, symbol = symbol, chosen = chosen) }
}
