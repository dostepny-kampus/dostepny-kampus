/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext

/**
 * Plik ustawień [name] aplikacji. Graf Hilt tworzy go raz na proces, bo tak wymaga DataStore dla jednego pliku.
 * Zapis i odczyt idą w [ioContext]; gdy kontekst nie ma dyspozytora, jak w testach Hilt, plik pracuje w wątku
 * wywołującym, więc test nie czeka na zapis oknem zegara. Uszkodzony plik zastępują puste ustawienia.
 *
 * @param name nazwa pliku ustawień bez rozszerzenia
 * @param ioContext kontekst pracy na plikach z grafu Hilt
 * @param callerContext praca w wątku wywołującym, gdy [ioContext] nie ma dyspozytora
 */
internal fun Context.settingsStore(
    name: String,
    ioContext: CoroutineContext,
    callerContext: CoroutineContext = Dispatchers.Unconfined,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = CoroutineScope(callerContext + ioContext + SupervisorJob()),
        produceFile = { preferencesDataStoreFile(name) },
    )
