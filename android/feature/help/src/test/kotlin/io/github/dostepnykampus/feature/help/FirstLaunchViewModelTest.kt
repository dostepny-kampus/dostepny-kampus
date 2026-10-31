/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.dostepnykampus.core.data.start.FirstLaunch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass
import kotlin.reflect.cast

/** Pierwsze uruchomienie w ViewModel na zapisie pierwszego uruchomienia w pamięci testu. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirstLaunchViewModelTest {
    private var reads = 0
    private var isHelpSeen = false
    private val firstLaunch =
        object : FirstLaunch {
            override suspend fun isFirstLaunch(): Boolean {
                reads++
                return !isHelpSeen
            }

            override suspend fun markHelpSeen() {
                isHelpSeen = true
            }
        }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun us5_1_first_launch_read_once() {
        val start = FirstLaunchViewModel(firstLaunch)
        assertEquals(true, start.firstLaunchState.value)
        assertEquals(true, start.firstLaunchState.value)
        assertEquals(1, reads)
    }

    @Test
    fun us5_1_help_seen_saved() {
        val start = FirstLaunchViewModel(firstLaunch)
        start.markHelpSeen()
        assertEquals(true, isHelpSeen)
        assertEquals(false, FirstLaunchViewModel(firstLaunch).firstLaunchState.value)
    }

    @Test
    fun us5_1_saved_after_cleared() {
        val saving = CompletableDeferred<Unit>()
        val slow =
            object : FirstLaunch by firstLaunch {
                override suspend fun markHelpSeen() {
                    saving.await()
                    firstLaunch.markHelpSeen()
                }
            }
        val store = ViewModelStore()
        val factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T =
                    modelClass.cast(FirstLaunchViewModel(slow))
            }
        ViewModelProvider.create(store, factory)[FirstLaunchViewModel::class].markHelpSeen()
        store.clear()
        saving.complete(Unit)
        assertEquals(true, isHelpSeen)
    }
}
