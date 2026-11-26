/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Czas zimnego startu aplikacji do pierwszej klatki, z oczekiwaniem na ekran mapy: z profilem bazowym, gdy aplikacja
 * go ma, i bez niego. Test z `WithoutProfile` w nazwie trafia do osobnej statystyki.
 *
 * Wymagania: NFR-11
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    /** Reguła pomiaru. */
    @get:Rule
    val rule = MacrobenchmarkRule()

    private var isPrepared = false

    /** Pomiar z profilem bazowym, gdy jest w aplikacji. */
    @Test
    fun measuresColdStart() = measure(CompilationMode.DEFAULT)

    /** Pomiar bez profilu bazowego, do porównania. */
    @Test
    fun measuresStartWithoutProfile() = measure(CompilationMode.None())

    private fun measure(mode: CompilationMode) =
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = mode,
            iterations = ITERATIONS,
            startupMode = StartupMode.COLD,
            setupBlock = {
                if (!isPrepared) {
                    skipInstruction()
                    isPrepared = true
                }
                pressHome()
            },
        ) {
            startActivityAndWait()
            waitForMap()
        }

    private companion object {
        const val ITERATIONS = 10
    }
}
