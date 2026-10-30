/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.map

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MapListChoiceRepositoryTest {
    @TempDir
    lateinit var directory: File

    // NFR-19, NFR-20: bez wyboru nic nie jest zapisane, a wybór „Mapa”, np. przy dużej czcionce, przetrwa
    // zamknięcie aplikacji.
    @Test
    fun nfr19_choice_survives_restart() {
        val isListBefore =
            withRepository { repository -> repository.savedChoice.first().also { repository.choose(false) } }

        assertEquals(listOf(null, false), listOf(isListBefore, withRepository { it.savedChoice.first() }))
    }

    private fun <T> withRepository(block: suspend (MapListChoiceRepository) -> T): T =
        runBlocking {
            val job = SupervisorJob()
            val file = File(directory, "mapa.preferences_pb")
            val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(job)) { file }
            val result = block(MapListChoiceRepository(dataStore))
            job.cancelAndJoin()
            result
        }
}
