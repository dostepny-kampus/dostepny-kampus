/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import timber.log.Timber

/**
 * Drzewo logów wersji debug: z linii „Ekran: <nazwa>”, którą nawigator zapisuje przy zmianie ekranu, ustawia
 * etykietę `Ekran` dla JankStats, żeby wolna klatka w logu miała nazwę ekranu. Inne linie ignoruje.
 *
 * Wymagania: NFR-57
 */
internal class ScreenLabelTree(private val label: (key: String, value: String) -> Unit) : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (message.startsWith(PREFIX)) {
            label(KEY, message.removePrefix(PREFIX))
        }
    }

    private companion object {
        const val PREFIX = "Ekran: "
        const val KEY = "Ekran"
    }
}
