/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.building.Entrance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Najbliższa winda wejścia pokazanego w arkuszu V1, z planów pięter z kopią w telefonie. Nowe wejście przerywa
 * szukanie windy poprzedniego.
 *
 * Wymagania: US3.3, US16.3, BR-25
 *
 * @param plans plany pięter z kopią w telefonie
 */
@HiltViewModel
class EntranceLiftViewModel
    @Inject
    constructor(plans: FloorPlans) : ViewModel() {
        private val chosen = MutableStateFlow<Entrance?>(null)

        /** Winda wejścia z [show]; `null` bez wejścia, bez windy i do końca szukania. */
        @OptIn(ExperimentalCoroutinesApi::class)
        val shown: StateFlow<EntranceLift?> =
            chosen
                .mapLatest { entrance -> entrance?.let { plans.liftNear(it) } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        /** Wybiera wejście [entrance], którego windę podaje [shown]. */
        fun show(entrance: Entrance) {
            chosen.value = entrance
        }
    }
