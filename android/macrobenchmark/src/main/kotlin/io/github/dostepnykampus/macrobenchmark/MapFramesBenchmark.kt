/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Czas klatek w scenariuszu: otwórz mapę, przesuń, zmień teren.
 *
 * Wymagania: NFR-11, NFR-56
 */
@RunWith(AndroidJUnit4::class)
class MapFramesBenchmark {
    /** Reguła pomiaru. */
    @get:Rule
    val rule = MacrobenchmarkRule()

    private var isPrepared = false

    /** Uruchamia pomiar scenariusza. */
    @Test
    fun measuresPanAndAreaSwitch() =
        rule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.DEFAULT,
            iterations = ITERATIONS,
            startupMode = StartupMode.WARM,
            setupBlock = {
                if (!isPrepared) {
                    skipInstruction()
                    isPrepared = true
                }
                pressHome()
                startActivityAndWait()
                waitForMap()
            },
        ) {
            panMap()
            switchArea()
        }

    private companion object {
        const val ITERATIONS = 8
    }
}
