/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.sync

import io.github.dostepnykampus.core.network.decodeContract
import io.github.dostepnykampus.core.network.inspection.toMarker
import kotlinx.serialization.json.Json
import io.github.dostepnykampus.core.network.json.SyncPullResponse as SyncPullResponseJson

/**
 * Zmiany z tekstu odpowiedzi [body] (`SyncPullResponse`); zgłoszenia i wejścia są pomijane.
 *
 * Wymagania: US15.1, US15.2
 *
 * @throws IllegalArgumentException gdy treść nie pasuje do kontraktu
 */
internal fun Json.decodeSyncPull(body: String): SyncPull {
    val response = decodeContract(SyncPullResponseJson.serializer(), body)
    return SyncPull(cursor = response.cursor, inspectionMarkers = response.inspectionMarkers.map { it.toMarker() })
}
