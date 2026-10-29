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
import kotlinx.coroutines.flow.first

private val Context.startDataStore by preferencesDataStore(
    name = "start",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Czy to pierwsze uruchomienie aplikacji: wtedy instrukcja V5 otwiera się sama, a potem już nie (US5.1). */
interface FirstLaunch {
    /**
     * Prawda, dopóki nie wywołano [markHelpSeen]. Sam odczyt niczego nie zapisuje, więc obrót telefonu
     * i odtworzenie procesu przed zamknięciem instrukcji dają ten sam wynik.
     */
    suspend fun isFirstLaunch(): Boolean

    /** Zapisuje, że osoba zamknęła instrukcję otwartą przy pierwszym uruchomieniu albo ją pominęła. */
    suspend fun markHelpSeen()
}

/** [FirstLaunch] zapisany w [dataStore], więc przetrwa zamknięcie aplikacji i restart telefonu. */
class FirstLaunchRepository(private val dataStore: DataStore<Preferences>) : FirstLaunch {
    override suspend fun isFirstLaunch(): Boolean = dataStore.data.first()[HELP_SEEN] != true

    override suspend fun markHelpSeen() {
        dataStore.edit { preferences -> preferences[HELP_SEEN] = true }
    }

    private companion object {
        val HELP_SEEN = booleanPreferencesKey("help_seen")
    }
}

/** [FirstLaunchRepository] na pliku ustawień startu aplikacji. */
fun Context.firstLaunchRepository(): FirstLaunchRepository = FirstLaunchRepository(applicationContext.startDataStore)
