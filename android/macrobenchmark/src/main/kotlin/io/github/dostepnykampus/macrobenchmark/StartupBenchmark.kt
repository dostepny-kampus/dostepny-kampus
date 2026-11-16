/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.macrobenchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Czas zimnego startu aplikacji do pierwszej klatki, z oczekiwaniem na ekran mapy.
 *
 * Wymagania: NFR-11
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    /** Reguła pomiaru. */
    @get:Rule
    val rule = MacrobenchmarkRule()

    /** Uruchamia pomiar scenariusza. */
    @Test
    fun measuresColdStart() =
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            iterations = ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait()
            waitForMap()
        }

    private companion object {
        const val ITERATIONS = 10
    }
}
