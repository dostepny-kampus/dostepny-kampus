/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.macrobenchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generuje profil bazowy ze scenariusza: otwórz mapę, przesuń, zmień teren. Plik `*-baseline-prof.txt` powstaje
 * w katalogu wyników przebiegu pomiarów; aplikacja go nie zawiera, bo pomiar nie pokazał przyspieszenia startu.
 *
 * Wymagania: NFR-11
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    /** Reguła zbierania profilu. */
    @get:Rule
    val rule = BaselineProfileRule()

    private var isPrepared = false

    /** Zbiera profil z całego scenariusza, aż kolejne przebiegi dadzą ten sam profil. */
    @Test
    fun generatesProfile() =
        rule.collect(packageName = TARGET_PACKAGE, maxIterations = MAX_ITERATIONS) {
            if (!isPrepared) {
                skipInstruction()
                isPrepared = true
            }
            pressHome()
            startActivityAndWait()
            waitForMap()
            panMap()
            switchArea()
        }

    private companion object {
        const val MAX_ITERATIONS = 6
    }
}
