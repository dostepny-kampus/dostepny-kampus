/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.model.Entrance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Wejście z „Zgłoś barierę przy wejściu” do kroku 1 V4, z katalogu budynków bez sieci. Wymagania: US4.6 */
@HiltViewModel
class QuickEntranceViewModel
    @Inject
    constructor(private val catalog: BuildingCatalogRepository) : ViewModel() {
        /** Wejście [id] albo `null`, gdy katalog go nie ma. */
        fun entrance(id: String): Flow<Entrance?> =
            catalog.catalogUpdates().map { updated -> updated.entrances.firstOrNull { it.id == id } }
    }
