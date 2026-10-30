/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.map

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.mapDataStore by preferencesDataStore(
    name = "mapa",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Ostatni wybór w przełączniku „Mapa” i „Lista budynków” na V1; aplikacja pamięta go po zamknięciu (NFR-19). */
interface MapListChoice {
    /** Prawda dla „Lista budynków”, fałsz dla „Mapa”, `null`, dopóki osoba nic nie wybrała. */
    val savedChoice: Flow<Boolean?>

    /** Zapisuje wybór osoby w przełączniku. */
    suspend fun choose(isList: Boolean)
}

/** [MapListChoice] zapisany w [dataStore], więc przetrwa zamknięcie aplikacji i restart telefonu. */
class MapListChoiceRepository(private val dataStore: DataStore<Preferences>) : MapListChoice {
    override val savedChoice: Flow<Boolean?> = dataStore.data.map { it[IS_LIST] }

    override suspend fun choose(isList: Boolean) {
        dataStore.edit { preferences -> preferences[IS_LIST] = isList }
    }

    private companion object {
        val IS_LIST = booleanPreferencesKey("is_list")
    }
}

/** [MapListChoiceRepository] na pliku ustawień mapy. */
fun Context.mapListChoice(): MapListChoiceRepository = MapListChoiceRepository(applicationContext.mapDataStore)
