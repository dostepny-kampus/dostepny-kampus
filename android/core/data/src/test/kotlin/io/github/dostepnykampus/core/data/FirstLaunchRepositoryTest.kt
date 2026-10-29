/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.dostepnykampus.core.data.start.FirstLaunchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FirstLaunchRepositoryTest {
    @TempDir
    lateinit var directory: File

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu
    @Test
    fun us5_1_help_until_marked_seen() {
        val launches =
            withRepository { repository ->
                val beforeMark = List(2) { repository.isFirstLaunch() }
                repository.markHelpSeen()
                beforeMark + repository.isFirstLaunch()
            }

        assertEquals(listOf(true, true, false), launches)
    }

    // US5.1: proces zamknięty, zanim osoba zamknęła instrukcję, więc po odtworzeniu instrukcja otwiera się znowu.
    @Test
    fun us5_1_unmarked_after_restart() {
        withRepository { it.isFirstLaunch() }

        assertTrue(withRepository { it.isFirstLaunch() })
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy
    @Test
    fun us5_1_second_launch_no_help() {
        withRepository { it.markHelpSeen() }

        assertFalse(withRepository { it.isFirstLaunch() })
    }

    private fun <T> withRepository(block: suspend (FirstLaunchRepository) -> T): T =
        runBlocking {
            val job = SupervisorJob()
            val file = File(directory, "start.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(job)) { file }
            val result = block(FirstLaunchRepository(dataStore))
            job.cancelAndJoin()
            result
        }
}
