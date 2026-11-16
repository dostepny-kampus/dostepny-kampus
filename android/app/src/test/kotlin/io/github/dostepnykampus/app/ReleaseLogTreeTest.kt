/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import timber.log.Timber

/**
 * Wersja release zapisuje do logcat tylko ostrzeżenia i błędy.
 *
 * Wymagania: NFR-57
 */
@RunWith(RobolectricTestRunner::class)
class ReleaseLogTreeTest {
    @Before
    fun plant() {
        ShadowLog.clear()
        Timber.plant(ReleaseLogTree())
    }

    @After
    fun uproot() {
        Timber.uprootAll()
    }

    @Test
    fun skipsDebugAndInfoMessages() {
        Timber.d("szczegół")
        Timber.i("informacja")

        assertEquals("Komunikaty DEBUG i INFO nie powinny trafić do logcat", 0, ShadowLog.getLogs().size)
    }

    @Test
    fun keepsWarningsAndErrors() {
        Timber.w("ostrzeżenie")
        Timber.e("błąd")

        assertEquals(
            "Ostrzeżenie i błąd powinny trafić do logcat",
            listOf(Log.WARN, Log.ERROR),
            ShadowLog.getLogs().map { entry -> entry.type },
        )
    }
}
