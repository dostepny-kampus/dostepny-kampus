/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.camera.MapCamera
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update

/**
 * Plan piętra pokazany na mapie.
 *
 * Wymagania: US16.1
 *
 * @property building symbol budynku z planem
 * @property floors piętra budynku od najniższego
 * @property floor identyfikator wybranego piętra
 * @property rooms pomieszczenia pięter z kopii planu, według identyfikatora piętra
 */
data class ShownPlan(
    val building: String,
    val floors: List<Floor>,
    val floor: String,
    val rooms: Map<String, RoomList>,
)

/**
 * Plan piętra przy bieżącym widoku mapy. Budynek wybiera [planFocus] z budynków szczegółowych katalogu (R2, R3).
 * Plan budynku pobiera się z serwera raz na udane pobranie, a bez sieci pochodzi z kopii [plans]. Na start jest
 * parter albo najniższe piętro (R5); wybrane piętro budynku zostaje zapamiętane na czas życia obiektu (R6).
 * Budynek bez kopii planu nie ma planu.
 *
 * TODO(#2412): piętro zalecanego wejścia na start (R5).
 *
 * Wymagania: US16.1, US16.2, BR-G1, NFR-10
 *
 * @param plans plany pięter z kopią w telefonie
 */
internal class MapPlan(private val plans: FloorPlans) {
    private val cameras = MutableStateFlow<MapCamera?>(null)
    private val chosen = MutableStateFlow<Map<String, String>>(emptyMap())
    private val downloaded = mutableSetOf<String>()

    /** Zapisuje widok mapy [view] po ruchu kamery. */
    fun camera(view: MapCamera) {
        cameras.value = view
    }

    /** Zapamiętuje piętro [floor] budynku [building]. */
    fun choose(building: String, floor: String) {
        chosen.update { it + (building to floor) }
    }

    /** Plan przy widoku mapy dla katalogów [catalogs]; `null`, gdy planu nie widać. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun shown(catalogs: Flow<BuildingCatalog?>): Flow<ShownPlan?> {
        val views = combine(flow = cameras.filterNotNull(), flow2 = catalogs) { view, catalog -> view to catalog }
        val focus =
            views.scan<Pair<MapCamera, BuildingCatalog?>, String?>(null) { shown, (view, catalog) ->
                val detailed = catalog?.run { buildings.filter { it.symbol in detailed } }.orEmpty()
                planFocus(center = view.center, zoom = view.zoom, detailed = detailed, shown = shown)
            }
        val distinct = focus.distinctUntilChanged()
        val loaded = distinct.mapLatest { symbol -> symbol?.let { load(it) } }
        return combine(flow = loaded, flow2 = chosen) { plan, floors ->
            plan?.run { copy(floor = floors[building]?.takeIf { id -> id in rooms } ?: floor) }
        }
    }

    private suspend fun load(symbol: String): ShownPlan? {
        if (symbol !in downloaded && plans.download(symbol)) downloaded += symbol
        val floors = plans.floors(symbol)?.floors.orEmpty()
        val rooms = floors.mapNotNull { floor -> plans.rooms(symbol, floor.id)?.let { floor.id to it } }.toMap()
        val start = floors.firstOrNull { it.order == 0 } ?: floors.firstOrNull()
        return start?.let { ShownPlan(building = symbol, floors = floors, floor = it.id, rooms = rooms) }
    }
}
