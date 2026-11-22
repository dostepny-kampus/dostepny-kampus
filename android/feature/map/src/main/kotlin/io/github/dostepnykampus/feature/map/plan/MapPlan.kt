/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.feature.map.building.orderedEntrances
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.navigation.MapFocus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update

/**
 * Plan piętra przy bieżącym widoku mapy. Budynek wybiera [planFocus] z budynków szczegółowych katalogu (R2, R3).
 * Plan budynku pobiera się z serwera raz na udane pobranie, a bez sieci pochodzi z kopii [plans]. Na start jest
 * piętro wejścia zalecanego z [entrances], a gdy dane go nie podają albo plan nie ma tego piętra — parter albo
 * najniższe piętro (R5); wybrane piętro budynku zostaje zapamiętane na czas życia obiektu (R6). Budynek bez kopii
 * planu nie ma planu. Bez mapy na ekranie ([hide]) planu nie ma do następnego widoku kamery.
 *
 * Wymagania: US16.1, US16.2, BR-G1, BR-25, NFR-10
 *
 * @param plans plany pięter z kopią w telefonie
 * @param entrances wejścia budynku z serwera z piętrem wejścia
 * @param barriers przyjęte bariery przy wejściach; wejście zalecane bierze je pod uwagę jak karta budynku
 */
internal class MapPlan(
    private val plans: FloorPlans,
    private val entrances: ServerEntrances,
    private val barriers: AcceptedBarriers = AcceptedBarriers { emptyList() },
) {
    private val cameras = MutableStateFlow<MapCamera?>(null)
    private val chosen = MutableStateFlow<Map<String, String>>(emptyMap())
    private val selected = MutableStateFlow<String?>(null)
    private val chosenRoom = MutableStateFlow<Pair<String, String>?>(null)
    private val downloaded = mutableSetOf<String>()
    private var shownFocus: MapFocus? = null

    /** Zapisuje widok mapy [view] po ruchu kamery. */
    fun camera(view: MapCamera) {
        cameras.value = view
    }

    /** Zapomina widok mapy, gdy mapa schodzi z ekranu; do następnego widoku planu nie ma. */
    fun hide() {
        cameras.value = null
    }

    /** Zapamiętuje piętro [floor] budynku [building]. */
    fun choose(building: String, floor: String) {
        chosen.update { it + (building to floor) }
    }

    /**
     * Pokazuje piętro z zaznaczenia [focus] raz na zaznaczenie; sala z zaznaczenia jest wyróżniona na planie swojego
     * budynku. Budynku, który trzyma plan, zaznaczenie nie wybiera: robi to [select].
     */
    fun show(focus: MapFocus?) {
        if (focus == shownFocus) return
        shownFocus = focus
        val building = focus as? MapFocus.Building
        building?.floor?.let { choose(building = building.symbol, floor = it) }
        chosenRoom.value = building?.room?.let { building.symbol to it }
    }

    /** Ustawia budynek [building] karty albo arkusza nad mapą, który trzyma plan; `null` wraca do środka mapy. */
    fun select(building: String?) {
        selected.value = building
    }

    /** Plan przy widoku mapy dla katalogów [catalogs]; `null`, gdy planu nie widać. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun shown(catalogs: Flow<BuildingCatalog?>): Flow<ShownPlan?> {
        val views = combine(flow = cameras, flow2 = catalogs, flow3 = selected, transform = ::PlanInput)
        val focus =
            views.scan<PlanInput, String?>(null) { shown, input ->
                val buildings = planBuildings(input.catalog)
                input.view?.let { view ->
                    planFocus(
                        center = view.center,
                        zoom = view.zoom,
                        buildings = buildings,
                        shown = shown,
                        selected = input.selected,
                    )
                }
            }
        val distinct = focus.distinctUntilChanged()
        val loaded = distinct.mapLatest { symbol -> symbol?.let { load(it) } }
        return combine(flow = loaded, flow2 = chosen, flow3 = chosenRoom) { plan, floors, room ->
            plan?.run {
                val chosenFloor = floors[building]?.takeIf { id -> id in rooms } ?: floor
                copy(floor = chosenFloor, room = room?.takeIf { it.first == building }?.second)
            }
        }
    }

    private fun planBuildings(catalog: BuildingCatalog?): PlanBuildings {
        if (catalog == null) return PlanBuildings(detailed = emptyList())
        val (withPlan, withoutPlan) = catalog.buildings.partition { it.symbol in catalog.detailed }
        return PlanBuildings(detailed = withPlan, basic = withoutPlan)
    }

    private suspend fun load(symbol: String): ShownPlan? {
        if (symbol !in downloaded && plans.download(symbol)) downloaded += symbol
        val floors = plans.floors(symbol)?.floors.orEmpty()
        val rooms = floors.mapNotNull { floor -> plans.rooms(symbol, floor.id)?.let { floor.id to it } }.toMap()
        val accepted = barriers.barriersOf(symbol).orEmpty()
        val recommended = orderedEntrances(entrances.entrances(symbol), accepted).firstOrNull { it.isRecommended }
        val entranceFloor = recommended?.run { entrance.floorId }
        val start =
            floors.firstOrNull { it.id == entranceFloor }
                ?: floors.firstOrNull { it.order == 0 }
                ?: floors.firstOrNull()
        return start?.let { ShownPlan(building = symbol, floors = floors, floor = it.id, rooms = rooms) }
    }
}

private data class PlanInput(val view: MapCamera?, val catalog: BuildingCatalog?, val selected: String?)

/** Numerowana sala [id] na widocznym piętrze albo `null`. Wymagania: US18.1 */
internal fun ShownPlan.roomAt(id: String): FoundRoom? {
    val level = floors.firstOrNull { it.id == floor } ?: return null
    val found = rooms[floor]?.run { rooms.firstOrNull { it.id == id && it.isLabelled && !it.number.isNullOrBlank() } }
    return found?.let { FoundRoom(building = building, floor = level, room = it) }
}
