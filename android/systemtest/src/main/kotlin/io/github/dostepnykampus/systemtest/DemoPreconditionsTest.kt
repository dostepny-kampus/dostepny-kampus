/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Założenia drogi demo na prawdziwym serwerze: A-1 z pięcioma piętrami z bazy testu, cztery z nich oznaczone
 * i jedno przyjęte TB-13 „próg”. Drugie przygotowanie na tym samym serwerze zostawia ten sam stan.
 *
 * Wymagania: US10.1, US11.1
 */
@RunWith(AndroidJUnit4::class)
class DemoPreconditionsTest {
    private val server = DemoServer()

    /**
     * Drugie przygotowanie założeń zostawia 4 z 5 pięter i jedno przyjęte TB-13.
     *
     * Scenariusz (częściowo): scenariusz-demo.feature: Schody przy wejściu głównym A-1 od pomiaru do eksportu
     *
     * Wymagania: US10.1, US11.1
     */
    @Test
    fun preparedTwiceIsTheSame() {
        server.prepare()
        server.prepare()
        val floors = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 4)
        assertEquals("Piętra A-1 obejrzane przez Zbierającego", floors, server.progress().coverage)
        val threshold = server.accepted(THRESHOLD_NOTE)
        assertEquals("Przyjęte TB-13 z założeń: $threshold", 1, threshold.size)
        assertEquals("Status TB-13", SubmissionStatus.ACCEPTED, threshold.single().status)
    }
}
