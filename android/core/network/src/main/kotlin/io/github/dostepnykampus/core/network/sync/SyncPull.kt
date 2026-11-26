/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.sync

import io.github.dostepnykampus.core.model.inspection.InspectionMarker

/**
 * Zmiany z serwera od poprzedniego pobrania (`GET /sync/pull`), w części czytanej przez aplikację.
 *
 * Wymagania: US15.1, US15.2
 *
 * @property cursor znacznik pobrania, który następne pobranie podaje jako `since`
 * @property inspectionMarkers znaczniki pięter wszystkich Zbierających, także cofnięte; pusta lista dla innych ról
 */
public data class SyncPull(val cursor: String, val inspectionMarkers: List<InspectionMarker>)
