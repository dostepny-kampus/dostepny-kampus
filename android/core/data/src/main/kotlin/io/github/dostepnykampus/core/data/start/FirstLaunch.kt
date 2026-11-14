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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/** Plik ustawień startu aplikacji; jeden DataStore na proces, bo tak wymaga DataStore dla jednego pliku. */
internal val Context.startDataStore by preferencesDataStore(
    name = "start",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/** Czy to pierwsze uruchomienie aplikacji: wtedy instrukcja V5 otwiera się sama, a potem już nie. Wymagania: US5.1 */
public interface FirstLaunch {
    /**
     * Prawda, dopóki nie wywołano [markHelpSeen]. Sam odczyt niczego nie zapisuje, więc obrót telefonu
     * i odtworzenie procesu przed zamknięciem instrukcji dają ten sam wynik. Gdy pliku nie da się odczytać,
     * zwraca fałsz: aplikacja startuje tak, jakby instrukcja była już pokazana.
     */
    public suspend fun isFirstLaunch(): Boolean

    /**
     * Zapisuje, że osoba zamknęła instrukcję otwartą przy pierwszym uruchomieniu albo ją pominęła.
     * Błąd zapisu nie wychodzi z tej metody.
     */
    public suspend fun markHelpSeen()
}

/** [FirstLaunch] zapisany w [dataStore], więc przetrwa zamknięcie aplikacji i restart telefonu. */
internal class FirstLaunchRepository(private val dataStore: DataStore<Preferences>) : FirstLaunch {
    override suspend fun isFirstLaunch(): Boolean {
        val readings =
            dataStore.data
                .map { preferences -> preferences[HELP_SEEN] != true }
                .catch { error ->
                    if (error is IOException) {
                        emit(false)
                    } else {
                        throw error
                    }
                }
        return readings.first()
    }

    override suspend fun markHelpSeen() {
        try {
            dataStore.edit { preferences -> preferences[HELP_SEEN] = true }
        } catch (_: IOException) {
        }
    }

    private companion object {
        val HELP_SEEN = booleanPreferencesKey("help_seen")
    }
}
