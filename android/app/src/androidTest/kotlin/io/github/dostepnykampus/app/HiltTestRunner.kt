/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import org.maplibre.android.MapLibre

/**
 * Uruchamia testy na urządzeniu w aplikacji testowej Hilt, więc graf testu może podmienić moduły aplikacji, np. sieć
 * na [NoServerNetworkModule].
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, HiltTestApplication::class.java.name, context)

    /**
     * Biblioteka mapy dostaje tylko deklarację „brak połączenia” (flaga);
     * styl nadal może pobierać kafelki z sieci.
     */
    override fun callApplicationOnCreate(app: Application?) {
        super.callApplicationOnCreate(app)
        app?.let { MapLibre.getInstance(it) }
        MapLibre.setConnected(false)
    }
}
