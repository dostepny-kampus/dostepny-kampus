/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.feature.map.camera.MapCamera
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Plan piętra na mapie V1 według [MapPlan] z budynkami szczegółowymi katalogu [repository]. Plan jest liczony, gdy
 * ekran jest widoczny, i 5 s po zniknięciu; wybrane piętra budynków zostają do zamknięcia ekranu.
 *
 * Wymagania: US16.1, NFR-10
 *
 * @param repository katalog budynków z listą budynków szczegółowych
 * @param plans plany pięter z kopią w telefonie
 */
@HiltViewModel
class MapPlanViewModel
    @Inject
    constructor(repository: BuildingCatalogRepository, plans: FloorPlans) : ViewModel() {
        private val plan = MapPlan(plans)

        /** Plan piętra budynku przy środku mapy; `null`, gdy planu nie widać. */
        val shown: StateFlow<ShownPlan?> =
            plan
                .shown(repository.catalogUpdates())
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        /** Zapisuje widok mapy [view] po ruchu kamery; od niego zależy plan piętra. */
        fun camera(view: MapCamera) {
            plan.camera(view)
        }

        /** Pokazuje piętro [floor] budynku [building] i zapamiętuje je. */
        fun chooseFloor(building: String, floor: String) {
            plan.choose(building, floor)
        }
    }
