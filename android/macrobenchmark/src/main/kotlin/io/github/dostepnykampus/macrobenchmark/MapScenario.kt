/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/** Pakiet mierzonej aplikacji. */
internal const val TARGET_PACKAGE: String = "io.github.dostepnykampus"

private const val MAP_TIMEOUT_MILLIS = 15_000L
private const val SWIPES = 4
private const val SWIPE_STEPS = 30
private const val LEFT_FRACTION = 4
private const val RIGHT_NUMERATOR = 3

/**
 * Czeka, aż mapa pokaże przycisk zmiany terenu, czyli aż widać ekran mapy kampusu.
 *
 * Wymagania: NFR-11
 */
internal fun MacrobenchmarkScope.waitForMap() {
    device.wait(Until.hasObject(By.descContains("Zmień teren")), MAP_TIMEOUT_MILLIS)
}

/**
 * Przesuwa mapę kilka razy w obie strony; brak mapy nie kończy się błędem, bo pomiar liczy klatki, nie poprawność.
 *
 * Wymagania: NFR-11
 */
internal fun MacrobenchmarkScope.panMap() {
    val width = device.displayWidth
    val height = device.displayHeight
    repeat(SWIPES) { index ->
        val fromLeft = index % 2 == 0
        val near = width / LEFT_FRACTION
        val far = width * RIGHT_NUMERATOR / LEFT_FRACTION
        device.swipe(if (fromLeft) near else far, height / 2, if (fromLeft) far else near, height / 2, SWIPE_STEPS)
        device.waitForIdle()
    }
}

/**
 * Otwiera panel terenów i wybiera pierwszy inny teren, jeśli taki jest; bez niego wraca bez zmiany.
 *
 * Wymagania: NFR-56
 */
internal fun MacrobenchmarkScope.switchArea() {
    device.findObject(By.descContains("Zmień teren"))?.click()
    device.wait(Until.hasObject(By.descContains("inny teren")), MAP_TIMEOUT_MILLIS)
    device.findObject(By.descContains("inny teren"))?.click()
    device.waitForIdle()
}
