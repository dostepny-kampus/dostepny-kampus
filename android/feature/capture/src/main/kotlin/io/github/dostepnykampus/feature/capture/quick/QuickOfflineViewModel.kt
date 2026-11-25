/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import java.time.Instant
import javax.inject.Inject

/**
 * Wiek danych mapy do linii „Bez internetu” na potwierdzeniu V4: chwila pobrania najstarszych danych mapy
 * z serwera, budynków albo barier, odczytana raz przy otwarciu potwierdzenia.
 *
 * Wymagania: US9.1, NFR-10
 *
 * @param catalog katalog budynków z chwilą pobrania z serwera
 * @param barriers bariery przyjęte z chwilą pobrania z serwera
 */
@HiltViewModel
class QuickOfflineViewModel
    @Inject
    constructor(
        catalog: BuildingCatalogRepository,
        barriers: AcceptedBarriers,
    ) : ViewModel() {
        /** Chwila pobrania najstarszych danych mapy; `null`, gdy telefon ma tylko dane z pakietu aplikacji. */
        val syncedAt: Instant? = listOfNotNull(catalog.syncedAt(), barriers.syncedAt()).minOrNull()
    }
