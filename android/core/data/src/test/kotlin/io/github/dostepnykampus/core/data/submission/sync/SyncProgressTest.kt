/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import androidx.work.ExistingWorkPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Postęp „Wysyłamy X z Y” i „Synchronizuj teraz” bez dublowania przebiegów. Wymagania: US9.1, BR-9 */
class SyncProgressTest {
    /** Przebieg liczy wysłane od liczby czekających na jego początku. Wymagania: US9.1 */
    @Test
    fun countsSentFromStart() {
        val start = nextProgress(last = null, isRunning = true, waiting = 5)
        val later = nextProgress(last = start, isRunning = true, waiting = 3)

        assertEquals(SyncProgress(sent = 0, total = 5), start, "początek przebiegu")
        assertEquals(SyncProgress(sent = 2, total = 5), later, "po dwóch wysłanych")
    }

    /** Zapis w trakcie przebiegu powiększa całość, a nie cofa wysłanych. Wymagania: US9.1, BR-9 */
    @Test
    fun growsWithSavesDuringRun() {
        val after = nextProgress(last = SyncProgress(sent = 2, total = 5), isRunning = true, waiting = 4)

        assertEquals(SyncProgress(sent = 2, total = 6), after, "nowe zgłoszenie dochodzi do całości")
    }

    /** Bez trwającego przebiegu nie ma postępu. Wymagania: US9.1 */
    @Test
    fun noProgressWhenIdle() {
        assertNull(nextProgress(last = SyncProgress(sent = 5, total = 5), isRunning = false, waiting = 0))
    }

    /** Strumień postępu idzie za przebiegiem i za kolejką. Wymagania: US9.1 */
    @Test
    fun followsRunAndQueue() =
        runTest(UnconfinedTestDispatcher()) {
            val isRunning = MutableStateFlow(false)
            val waiting = MutableStateFlow(3)
            val seen = mutableListOf<SyncProgress?>()
            val job = launch { progressOf(isRunning = isRunning, waiting = waiting).toList(seen) }

            isRunning.value = true
            waiting.value = 2
            waiting.value = 0
            isRunning.value = false
            job.cancel()

            val expected =
                listOf(
                    null,
                    SyncProgress(sent = 0, total = 3),
                    SyncProgress(sent = 1, total = 3),
                    SyncProgress(sent = 3, total = 3),
                    null,
                )
            assertEquals(expected, seen, "kolejne stany postępu")
        }

    /** Trwający przebieg zostaje, inaczej nowe zadanie zastępuje czekające. Wymagania: US9.1, BR-9 */
    @Test
    fun startsOnlyWhenIdle() {
        assertNull(syncNowPolicy(isRunning = true), "trwający przebieg nie jest dublowany")
        assertEquals(ExistingWorkPolicy.REPLACE, syncNowPolicy(isRunning = false), "czekające zadanie rusza od razu")
    }
}
