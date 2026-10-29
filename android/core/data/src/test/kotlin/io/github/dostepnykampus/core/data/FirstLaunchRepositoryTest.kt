/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.start.FirstLaunchRepository
import io.github.dostepnykampus.core.model.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FirstLaunchRepositoryTest {
    @TempDir
    lateinit var directory: File

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcja przy pierwszym uruchomieniu
    @Test
    fun us5_1_help_only_first_launch() {
        val launches = withRepository { repository -> List(3) { repository.consume() } }

        assertEquals(listOf(true, false, false), launches)
    }

    // Scenariusz (częściowo): przegladanie-mapy.feature: Instrukcję można otworzyć ponownie z pomocy
    @Test
    fun us1_2_restart_opens_map() {
        withRepository { it.consume() }

        assertEquals(false, withRepository { it.consume() })
    }

    @Test
    fun us14_2_everyone_is_guest() {
        assertEquals(Account.Guest, GuestRoleProvider.currentAccount())
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
