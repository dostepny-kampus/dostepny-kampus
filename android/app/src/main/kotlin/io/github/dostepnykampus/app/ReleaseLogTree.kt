/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.util.Log
import timber.log.Timber

/**
 * Drzewo logów wersji release: do logcat trafiają tylko ostrzeżenia i błędy, tag to nazwa klasy wołającej.
 *
 * Wymagania: NFR-57
 */
internal class ReleaseLogTree : Timber.DebugTree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN
}
