/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Zasłonięcie dołu mapy przez zamkniętą kartę bez kompozycji, klatka po klatce. Wymagania: NFR-56, US2.8 */
class SheetEdgeTest {
    /** Wiersz: V1, „Zamknij” na małej karcie — w klatce zamknięcia, przed zjazdem, karta dalej zasłania dół mapy. */
    @Test
    fun coverHoldsUntilSlide() {
        val exit = SheetExit()
        val edge = closedEdge(exit)
        assertEquals("klatka zamknięcia przed zjazdem", HEIGHT, edge.shown())
    }

    /** Wiersz: V1, „Zamknij” na małej karcie — w czasie zjazdu zasłonięcie tylko maleje, od wysokości karty do zera. */
    @Test
    fun coverShrinksDuringSlide() {
        val exit = SheetExit()
        val edge = closedEdge(exit)
        val covers = mutableListOf(edge.shown())
        runBlocking(StepClock { covers += edge.shown() }) { exit.leave() }
        assertEquals("zjazd zaczyna od wysokości karty: $covers", HEIGHT, covers.first())
        assertTrue("zasłonięcie tylko maleje: $covers", covers.zipWithNext().all { (before, after) -> after <= before })
        assertEquals("po zjeździe mapa odsłonięta: $covers", 0, covers.last())
    }

    private fun closedEdge(exit: SheetExit): SheetEdge {
        val edge = SheetEdge(exit)
        exit.keep(height = HEIGHT, offset = 0f)
        exit.mark(isShown = true)
        edge.keep(cover = HEIGHT, hidden = { 0f })
        edge.keep(cover = HEIGHT, hidden = null)
        exit.mark(isShown = false)
        return edge
    }

    private class StepClock(private val afterFrame: () -> Unit) : MonotonicFrameClock {
        private var nanos = 0L

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            nanos += FRAME_NANOS
            return onFrame(nanos).also { afterFrame() }
        }
    }

    private companion object {
        const val HEIGHT = 600
        const val FRAME_NANOS = 16_000_000L
    }
}
