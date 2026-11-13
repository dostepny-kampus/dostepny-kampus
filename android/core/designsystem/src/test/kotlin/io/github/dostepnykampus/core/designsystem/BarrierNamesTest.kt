/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Każdy typ, grupa i uciążliwość mają własną nazwę ze słownika projektu. Wymagania: BR-7, US7.1 */
@RunWith(RobolectricTestRunner::class)
class BarrierNamesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun typesHaveDistinctNames() {
        val names = BarrierType.entries.map { context.getString(it.nameRes()) }

        assertEquals(BarrierType.entries.size, names.toSet().size)
        assertEquals("Schody przy wejściu", names.first())
        assertEquals("Próg", context.getString(BarrierType.THRESHOLD.nameRes()))
        assertEquals("Inna bariera", names.last())
    }

    @Test
    fun groupsHaveDistinctNames() {
        assertEquals(
            listOf(
                "Wejście i drzwi",
                "Komunikacja pionowa",
                "Komunikacja pozioma",
                "Toalety",
                "Orientacja i informacja",
                "Inne",
            ),
            BarrierGroup.entries.map { context.getString(it.nameRes()) },
        )
    }

    @Test
    fun severitiesAsOnMockup() {
        assertEquals(
            listOf("Utrudnienie", "Tylko z pomocą", "Blokada"),
            Severity.entries.map { context.getString(it.nameRes()) },
        )
        assertEquals(
            "Osoba nie pokona jej wcale albo tylko wtedy, gdy ktoś ją przeniesie.",
            context.getString(Severity.BLOCKING.descriptionRes()),
        )
    }
}
