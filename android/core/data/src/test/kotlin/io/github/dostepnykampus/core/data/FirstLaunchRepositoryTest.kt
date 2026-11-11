/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.dostepnykampus.core.data.start.FirstLaunchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class FirstLaunchRepositoryTest {
    @TempDir
    lateinit var directory: File

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu. */
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

    /** US5.1: proces zamknięty, zanim osoba zamknęła instrukcję, więc po odtworzeniu instrukcja otwiera się znowu. */
    @Test
    fun us5_1_unmarked_after_restart() {
        withRepository { it.isFirstLaunch() }

        assertTrue(withRepository { it.isFirstLaunch() })
    }

    /** Błąd odczytu pliku ustawień nie zamyka aplikacji i nie otwiera instrukcji od nowa. */
    @Test
    fun us5_1_read_error_skips_help() {
        assertFalse(runBlocking { FirstLaunchRepository(UnreadableSettings()).isFirstLaunch() })
    }

    /** Błąd zapisu pliku ustawień zostaje w repozytorium. */
    @Test
    fun us5_1_write_error_is_kept() {
        runBlocking { FirstLaunchRepository(UnreadableSettings()).markHelpSeen() }
    }

    /** Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy. */
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

/** DataStore, którego odczyt i zapis kończą się [IOException]. */
private class UnreadableSettings : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("odczyt") }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        throw IOException("zapis")
}
