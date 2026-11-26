/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.map

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Plik ustawień mapy. DataStore wymaga jednej instancji na plik, więc wołający trzyma wynik jako
 * jedyny w swoim grafie; nowy graf (inny proces, inny test) dostaje nową instancję.
 */
internal fun mapDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile("mapa") },
    )

/**
 * Ostatni widok V1 wybrany przez osobę, mapa albo lista; aplikacja pamięta go po zamknięciu.
 *
 * Wymagania: NFR-19
 */
public interface MapListChoice {
    /** Prawda dla listy, fałsz dla mapy, `null`, dopóki osoba nic nie wybrała. */
    public val savedChoice: Flow<Boolean?>

    /** Zapisuje widok wybrany przez osobę. */
    public suspend fun choose(isList: Boolean)
}

/** [MapListChoice] zapisany w [dataStore], więc przetrwa zamknięcie aplikacji i restart telefonu. */
internal class MapListChoiceRepository(private val dataStore: DataStore<Preferences>) : MapListChoice {
    override val savedChoice: Flow<Boolean?> = dataStore.data.map { it[IS_LIST] }

    override suspend fun choose(isList: Boolean) {
        dataStore.edit { preferences -> preferences[IS_LIST] = isList }
    }

    private companion object {
        val IS_LIST = booleanPreferencesKey("is_list")
    }
}
