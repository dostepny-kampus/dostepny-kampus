/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.server.toReply
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerDao
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerEntity
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.sync.SyncPull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Plik stanu pobierania zmian z serwera. DataStore wymaga jednej instancji na plik, więc wołający trzyma wynik jako
 * jedyny w swoim grafie.
 */
internal fun syncDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile("synchronizacja") },
    )

/** Znaczniki pięter zapisane na serwerze z kopią w bazie telefonu. Wymagania: US15.1, US15.2, BR-20 */
public interface ServerMarkers {
    /** Znaczniki budynku [buildingSymbol] z ostatniego pobrania, bez cofniętych; działa bez sieci. */
    public fun building(buildingSymbol: String): Flow<List<InspectionMarker>>

    /** Pobiera zmiany od poprzedniego pobrania (`GET /sync/pull`): nowe znaczniki zapisuje, cofnięte usuwa. */
    public suspend fun refresh(): ServerReply<Unit>
}

/**
 * [ServerMarkers] z kopią w bazie telefonu i kursorem poprzedniego pobrania.
 *
 * @param markers tabela znaczników z serwera
 * @param state plik z kursorem poprzedniego pobrania
 * @param pull pobranie zmian od kursora; `null` pobiera wszystko
 */
internal class SyncedMarkers(
    private val markers: InspectionMarkerDao,
    private val state: DataStore<Preferences>,
    private val pull: suspend (String?) -> ApiResult<SyncPull>,
) : ServerMarkers {
    override fun building(buildingSymbol: String): Flow<List<InspectionMarker>> =
        markers.observe(buildingSymbol).map { saved -> saved.map { it.toMarker() } }

    override suspend fun refresh(): ServerReply<Unit> {
        val since = state.data.first()[CURSOR]
        val result = pull(since)
        val reply = result.toReply { it }
        if (reply is ServerReply.Ok) {
            val (revoked, kept) = reply.value.inspectionMarkers.partition { it.isRevoked }
            markers.apply(saved = kept.map { it.toEntity() }, revokedIds = revoked.map { it.id })
            state.edit { preferences -> preferences[CURSOR] = reply.value.cursor }
        }
        return result.toReply { }
    }

    private companion object {
        val CURSOR = stringPreferencesKey("kursor")
    }
}

private fun InspectionMarker.toEntity(): InspectionMarkerEntity =
    InspectionMarkerEntity(
        id = id,
        buildingSymbol = buildingSymbol,
        floorId = (place as? MarkerPlace.Floor)?.floorId,
        createdAtMillis = createdAt.toEpochMilli(),
        isMine = isMine,
    )

private fun InspectionMarkerEntity.toMarker(): InspectionMarker =
    InspectionMarker(
        id = id,
        buildingSymbol = buildingSymbol,
        place = floorId?.let { MarkerPlace.Floor(it) } ?: MarkerPlace.WholeBuilding,
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        isRevoked = false,
        isMine = isMine,
    )
