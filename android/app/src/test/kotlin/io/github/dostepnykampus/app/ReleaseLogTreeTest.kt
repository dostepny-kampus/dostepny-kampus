/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.util.Log
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Wersja release zapisuje do logcat tylko ostrzeżenia i błędy.
 *
 * Wymagania: NFR-57
 */
class ReleaseLogTreeTest {
    private val tree = ReleaseLogTree()

    @Test
    fun skipsMessagesBelowWarning() {
        val logged = listOf(Log.VERBOSE, Log.DEBUG, Log.INFO).map { priority -> tree.isLoggable(null, priority) }

        assertEquals("Komunikaty poniżej WARN nie powinny trafić do logcat", listOf(false, false, false), logged)
    }

    @Test
    fun keepsWarningsAndErrors() {
        val logged = listOf(Log.WARN, Log.ERROR, Log.ASSERT).map { priority -> tree.isLoggable(null, priority) }

        assertEquals("Ostrzeżenia i błędy powinny trafić do logcat", listOf(true, true, true), logged)
    }
}
