/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant

/**
 * Wynik przebiegów wysyłki.
 *
 * @property lastSyncedAt chwila ostatniego przebiegu, który doszedł do końca kolejki; `null` przed pierwszym
 * @property isLastRunFailed czy ostatni przebieg przerwał się przed końcem kolejki
 */
internal data class SyncRun(val lastSyncedAt: Instant?, val isLastRunFailed: Boolean)

/**
 * Plik dziennika przebiegów wysyłki. DataStore wymaga jednej instancji na plik, więc wołający trzyma wynik jako
 * jedyny w swoim grafie.
 */
internal fun syncDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile("synchronizacja") },
    )

/**
 * Dziennik przebiegów wysyłki w [dataStore]; przetrwa zamknięcie aplikacji. Błąd odczytu daje stan sprzed
 * pierwszego przebiegu, a błąd zapisu nie wychodzi poza dziennik.
 *
 * Wymagania: US9.1, NFR-10
 */
internal class SyncLog(private val dataStore: DataStore<Preferences>) {
    /** Wynik przebiegów, zmieniający się po każdym [record]. */
    fun runs(): Flow<SyncRun> =
        dataStore.data
            .map { preferences ->
                SyncRun(
                    lastSyncedAt = preferences[LAST_SYNCED_AT]?.let(Instant::ofEpochMilli),
                    isLastRunFailed = preferences[LAST_RUN_FAILED] == true,
                )
            }.catch { error ->
                if (error !is IOException) throw error
                emit(SyncRun(lastSyncedAt = null, isLastRunFailed = false))
            }

    /** Zapisuje przebieg z chwili [at]; tylko przebieg do końca kolejki ([isDone]) zmienia chwilę synchronizacji. */
    suspend fun record(isDone: Boolean, at: Instant) {
        try {
            dataStore.edit { preferences ->
                preferences[LAST_RUN_FAILED] = !isDone
                if (isDone) preferences[LAST_SYNCED_AT] = at.toEpochMilli()
            }
        } catch (_: IOException) {
        }
    }

    private companion object {
        val LAST_SYNCED_AT = longPreferencesKey("last_synced_at")
        val LAST_RUN_FAILED = booleanPreferencesKey("last_run_failed")
    }
}
