/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import io.github.dostepnykampus.feature.map.campus.CampusMapData
import io.github.dostepnykampus.feature.map.campus.MapArea
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import timber.log.Timber

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
 * @param symbol budynek zaznaczenia z trasy; dopóki teren nie jest ustawiony, to on wyznacza teren
 * @param chosen zapisywana nazwa terenu pokazanego na mapie: wybranego przez osobę albo ustawionego jednorazowo
 *   przez wybór budynku z innego terenu; `null`, dopóki mapa stoi na terenie startowym
 * @param seen zapisywana nazwa terenu, o którym osoba już wie: wybrała go albo ruszyła na nim mapę; `null`, dopóki
 *   widziała tylko teren startowy
 * @param followed zapisywany symbol budynku, którego teren mapa już pokazała; `null` bez zaznaczenia
 * @param noticeMemory co wiadomo o pokazywaniu napisu o zmianie terenu
 */
internal class MapAreas(
    private val data: CampusMapData?,
    private val symbol: String?,
    private val chosen: MutableState<String?>,
    private val seen: MutableState<String?>,
    private val followed: MutableState<String?>,
    private val noticeMemory: NoticeMemory = NoticeMemory(),
) {
    /**
     * Nazwa terenu ustawionego przez osobę albo wyborem budynku; `null`, gdy mapa stoi na terenie startowym. Nowy
     * budynek z innego terenu daje swój teren już w klatce wyboru, zanim [followSelection] go zapisze.
     */
    val chosenName: String? get() = arriving()?.name ?: chosen.value

    /** Teren pokazany na mapie; `null` bez danych mapy. */
    val current: MapArea? get() = data?.areaOf(chosenName, symbol)

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
     * Zmiana terenu do napisu „Pokazuję teren: Wittigowo · 2 km na południowy wschód”: mapa zmieniła teren sama,
     * bo zaznaczony budynek, np. z wyszukiwania albo z listy, leży na innym terenie niż ten, o którym osoba wie.
     * Odległość i kierunek liczą się od terenu, o którym osoba wiedziała. `null`, gdy teren wybrała osoba albo już
     * o nim wie.
     *
     * Wymagania: US1.8, NFR-56
     */
    val notice: AreaShift?
        get() {
            val known = data?.areaOf(chosen = seen.value, symbol = null)
            val shown = current
            return if (known == null || shown == null || shown == known) null else known.shiftTo(shown)
        }

    /**
     * Napis o zmianie terenu, który da zaznaczony budynek: [notice], a zanim mapa przejdzie na teren budynku,
     * zmiana z terenu znanego osobie na teren budynku. `null` bez zaznaczenia albo bez zmiany terenu.
     *
     * Wymagania: US1.8, US2.8
     */
    val selectionNotice: AreaShift?
        get() {
            val selected = symbol ?: return null
            if (followed.value == selected) return notice
            val known = data?.areaOf(chosen = seen.value, symbol = null)
            val target = data?.areaOf(chosen = null, symbol = selected)
            return if (known == null || target == null || known == target) null else known.shiftTo(target)
        }

    /**
     * Czy napis [notice] jest pokazywany pierwszy raz. Ten sam napis pokazany znów, np. po zamknięciu panelu,
     * nie jest nowy.
     *
     * Wymagania: US1.8, NFR-18
     */
    val isNoticeNew: Boolean get() = notice != null && noticeMemory.announced.value != current?.name

    /**
     * Napis o zmianie terenu wszedł na ekran ([isShown]) albo z niego zszedł. Pokazany napis jest już przeczytany.
     *
     * Wymagania: US1.8, NFR-18
     */
    fun noticeShown(isShown: Boolean) {
        noticeMemory.onScreen.value = isShown
        if (isShown) noticeMemory.announced.value = current?.name
    }

    /**
     * Osoba ruszyła mapę przyciskiem albo gestem. Gdy napis o zmianie terenu był wtedy na ekranie, osoba wie
     * o pokazanym terenie i napis znika; ruch przy zasłoniętym napisie go nie gasi.
     *
     * Wymagania: US1.8, NFR-23
     */
    fun acknowledge() {
        if (noticeMemory.onScreen.value) current?.let { seen.value = it.name }
    }

    /**
     * Zdarzenie zmiany zaznaczenia: nowy budynek ustawia teren mapy na jego teren, raz. Dalej teren zmienia
     * tylko osoba: zmiana albo zniknięcie zaznaczenia go nie rusza, a ten sam budynek nie ustawia go ponownie.
     *
     * Wymagania: US1.8, BR-G5
     */
    fun followSelection() {
        val selected = symbol
        if (selected == null) {
            followed.value = null
            return
        }
        val area = data?.areaOf(chosen = null, symbol = selected) ?: return
        if (followed.value != selected) {
            if (chosen.value != area.name) Timber.i("Mapa: teren %s po wyborze budynku %s", area.name, selected)
            chosen.value = area.name
            followed.value = selected
        }
    }

    private fun arriving(): MapArea? =
        symbol?.takeIf { it != followed.value }?.let { data?.areaOf(chosen = null, symbol = it) }

    /**
     * Wybiera teren [name] i zwraca prostokąt wszystkich jego budynków, który mapa pokazuje po wyborze.
     * `null` i brak zmiany, gdy dane nie mają takiego terenu.
     *
     * Wymagania: US1.7, BR-G5
     */
    fun show(name: String): CampusBounds? {
        val campus = data ?: return null
        val area = campus.areas.firstOrNull { it.name == name }
        if (area != null) {
            Timber.i("Mapa: teren %s wybrany przez osobę", name)
            chosen.value = name
            seen.value = name
        }
        return area?.let(campus::boundsOfAllBuildings)
    }
}

/**
 * Pamięć napisu o zmianie terenu.
 *
 * Wymagania: US1.8, NFR-18, NFR-23
 *
 * @property announced zapisywana nazwa terenu, którego napis był już pokazany; `null` przed pierwszym napisem
 * @property onScreen czy napis o zmianie terenu jest teraz na ekranie
 */
internal data class NoticeMemory(
    val announced: MutableState<String?> = mutableStateOf(null),
    val onScreen: MutableState<Boolean> = mutableStateOf(false),
)

/**
 * Tereny mapy z danych [layers] i zaznaczenia [focus] z trasy. Teren jest zapisywany, więc wraca po obrocie
 * ekranu i po „Wstecz” z V2. Wybór budynku z innego terenu, np. z wyszukiwania, ustawia teren raz; potem zmienia go
 * tylko osoba, a zamknięcie karty i zniknięcie zaznaczenia go zostawiają.
 *
 * Wymagania: NFR-22, US1.8
 */
@Composable
internal fun rememberMapAreas(layers: CampusMapData?, focus: MapFocus?): MapAreas {
    val chosen = rememberSaveable { mutableStateOf<String?>(null) }
    val followed = rememberSaveable { mutableStateOf<String?>(null) }
    val symbol =
        when (focus) {
            is MapFocus.Building -> focus.symbol
            is MapFocus.Entrance -> focus.buildingSymbol
            null -> null
        }
    val seen = rememberSaveable { mutableStateOf<String?>(null) }
    val announced = rememberSaveable { mutableStateOf<String?>(null) }
    val onScreen = remember { mutableStateOf(false) }
    val noticeMemory = remember { NoticeMemory(announced = announced, onScreen = onScreen) }
    val areas =
        remember(layers, symbol, chosen) {
            MapAreas(
                data = layers,
                symbol = symbol,
                chosen = chosen,
                seen = seen,
                followed = followed,
                noticeMemory = noticeMemory,
            )
        }
    SideEffect { areas.followSelection() }
    return areas
}

private fun CampusMapData.boundsOfAllBuildings(area: MapArea): CampusBounds {
    val shapes = area.symbols.mapNotNull(outlinesBySymbol::get)
    if (shapes.isEmpty()) return area.bounds
    return CampusBounds(
        west = shapes.minOf { it.west },
        south = shapes.minOf { it.south },
        east = shapes.maxOf { it.east },
        north = shapes.maxOf { it.north },
    )
}
