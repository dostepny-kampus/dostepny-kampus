/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.location

import org.junit.Assert.assertEquals
import org.junit.Test

/** Wersja release nie zawiera trybu pokazu: klas trasy ani włącznika nie ma w jej kodzie. Wymagania: US6.1 */
class ReleaseWithoutDemoTest {
    @Test
    fun hasNoDemoClasses() {
        val present = DEMO_CLASSES.filter { name -> exists("$PACKAGE.$name") }
        assertEquals("klasy trybu pokazu w release", emptyList<String>(), present)
    }

    private fun exists(name: String): Boolean = runCatching { Class.forName(name) }.isSuccess

    private companion object {
        const val PACKAGE = "io.github.dostepnykampus.core.location"
        val DEMO_CLASSES = listOf("DemoMode", "DemoRoute", "DemoFix", "DemoSatellite", "TrybPokazu")
    }
}
