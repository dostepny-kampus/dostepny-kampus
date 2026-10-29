/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.start

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.startDataStore by preferencesDataStore(
    name = "start",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Czy to pierwsze uruchomienie aplikacji: wtedy instrukcja V5 otwiera się sama, a potem już nie (US5.1). */
fun interface FirstLaunch {
    /** Prawda tylko przy pierwszym wywołaniu po instalacji; od razu zapisuje, że uruchomienie już było. */
    suspend fun consume(): Boolean
}

/** [FirstLaunch] zapisany w [dataStore], więc przetrwa zamknięcie aplikacji i restart telefonu. */
class FirstLaunchRepository(private val dataStore: DataStore<Preferences>) : FirstLaunch {
    override suspend fun consume(): Boolean {
        var isFirst = false
        dataStore.edit { preferences ->
            isFirst = preferences[HELP_SHOWN] != true
            preferences[HELP_SHOWN] = true
        }
        return isFirst
    }

    private companion object {
        val HELP_SHOWN = booleanPreferencesKey("help_shown")
    }
}

/** [FirstLaunchRepository] na pliku ustawień startu aplikacji. */
fun Context.firstLaunchRepository(): FirstLaunchRepository = FirstLaunchRepository(applicationContext.startDataStore)
