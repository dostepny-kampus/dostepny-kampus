/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.MapCamera
import io.github.dostepnykampus.feature.map.START_ZOOM
import io.github.dostepnykampus.feature.map.data.AreaChoice
import io.github.dostepnykampus.feature.map.data.AreaNeighbor
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapArea
import io.github.dostepnykampus.feature.map.data.areaChoices
import io.github.dostepnykampus.feature.map.data.areaNeighbor
import io.github.dostepnykampus.feature.map.data.areaOf
import io.github.dostepnykampus.feature.map.data.center
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import io.github.dostepnykampus.feature.map.style.property

/**
 * Tereny w kolejności nagłówków listy budynków: pokazany na mapie pierwszy, pozostałe od najbliższego.
 *
 * Wymagania: US1.8
 *
 * @property areas tereny z symbolami swoich budynków; puste, dopóki nie ma danych mapy
 */
@Immutable
internal data class AreaOrder(val areas: List<MapArea>)

/** Nazwa terenu budynku [symbol], gdy to inny teren niż pokazany na mapie; inaczej `null`. Wymagania: US1.8 */
internal fun AreaOrder.otherAreaOf(symbol: String): String? =
    areas.drop(n = 1).firstOrNull { symbol in it.symbols }?.name

/**
 * Tereny mapy V1: który teren mapa pokazuje i na jakie można go zmienić.
 *
 * Wymagania: US1.6, US1.7, US1.8
 *
 * @param data dane mapy z terenami; `null`, dopóki ich nie ma
 * @param symbol budynek zaznaczenia z trasy; bez wyboru osoby to on wyznacza teren
 * @param chosen zapisywana nazwa terenu wybranego przez osobę; `null` bez wyboru
 * @param seen zapisywana nazwa terenu, o którym osoba już wie: wybrała go albo ruszyła na nim mapę; `null`, dopóki
 *   widziała tylko teren startowy
 */
internal class MapAreas(
    private val data: CampusMapData?,
    private val symbol: String?,
    private val chosen: MutableState<String?>,
    private val seen: MutableState<String?>,
) {
    /** Nazwa terenu wybranego przez osobę; `null`, gdy teren wynika z zaznaczenia albo jest startowy. */
    val chosenName: String? get() = chosen.value

    /** Teren pokazany na mapie; `null` bez danych mapy. */
    val current: MapArea? get() = data?.areaOf(chosen.value, symbol)

    /**
     * Lista terenów: pokazany pierwszy, pozostałe od najbliższego, z liczbą budynków i odległością.
     *
     * Wymagania: US1.7
     */
    val choices: List<AreaChoice> get() = current?.let { data?.areaChoices(it) }.orEmpty()

    /** Tereny w kolejności nagłówków listy budynków: pokazany pierwszy, pozostałe od najbliższego. Wymagania: US1.8 */
    val order: AreaOrder
        get() {
            val known = data?.areas.orEmpty().associateBy(MapArea::name)
            return AreaOrder(choices.mapNotNull { known[it.name] })
        }

    /**
     * Nazwa terenu do napisu „Pokazuję teren: Wittigowo”: mapa zmieniła teren sama, bo zaznaczony budynek, np.
     * z wyszukiwania albo z listy, leży na innym terenie niż ten, o którym osoba wie. `null`, gdy teren
     * wybrała osoba albo już o nim wie.
     *
     * Wymagania: US1.8
     */
    val notice: String?
        get() {
            val known = seen.value ?: data?.areaOf(chosen = null, symbol = null)?.name
            val shown = current?.name
            return shown.takeIf { chosen.value == null && it != known }
        }

    /** Osoba wie o pokazanym terenie, np. ruszyła mapę przyciskiem: napis o zmianie terenu znika. Wymagania: US1.8 */
    fun acknowledge() {
        current?.let { seen.value = it.name }
    }

    /**
     * Sąsiedni teren dla środka widoku [view] na granicy pokazanego terenu; `null` z dala od granicy.
     *
     * Wymagania: US1.7
     */
    fun neighbor(view: GeoPoint): AreaNeighbor? = current?.let { data?.areaNeighbor(it, view) }

    /**
     * Wybiera teren [name] i zwraca jego widok startowy: środek terenu w powiększeniu startowym.
     * `null` i brak zmiany, gdy dane nie mają takiego terenu.
     *
     * Wymagania: US1.7, BR-G5
     */
    fun show(name: String): MapCamera? =
        data?.areas?.firstOrNull { it.name == name }?.let { area ->
            chosen.value = name
            seen.value = name
            MapCamera(center = area.bounds.center, zoom = START_ZOOM)
        }
}

/**
 * Tereny mapy z danych [layers] i zaznaczenia [focus] z trasy. Wybór osoby jest zapisywany, więc wraca po obrocie
 * ekranu i po „Wstecz” z V2; nowe zaznaczenie z trasy, np. budynek z wyszukiwania, znów wyznacza teren.
 *
 * Wymagania: NFR-22, US1.8
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
    val seen = rememberSaveable { mutableStateOf<String?>(null) }
    return remember(layers, symbol, chosen) { MapAreas(data = layers, symbol = symbol, chosen = chosen, seen = seen) }
}
