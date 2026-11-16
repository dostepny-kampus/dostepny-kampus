/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import android.app.Application
import android.content.Context
import android.os.StrictMode
import androidx.startup.Initializer
import timber.log.Timber

/**
 * Narzędzia diagnostyczne wersji debug, uruchamiane przez `androidx.startup` przy starcie procesu: StrictMode
 * zgłasza w logcat dysk i sieć na wątku głównym, a JankStats zapisuje w logcat wolne klatki z etykietami stanu
 * ustawianymi przez [ScreenLabelTree]. Narzędzia nic nie wysyłają poza telefon i nie zamykają procesu.
 * Wersja release ich nie zawiera.
 *
 * Wymagania: NFR-28, NFR-57
 */
class DebugToolsInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy
                .Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build(),
        )
        val tracking = JankTracking()
        Timber.plant(ScreenLabelTree(tracking::label))
        (context.applicationContext as Application).registerActivityLifecycleCallbacks(tracking)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
