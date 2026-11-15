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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * Pierwsze uruchomienie w ViewModel na zapisie pierwszego uruchomienia w pamięci testu.
 *
 * Wymagania: US5.1
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirstLaunchViewModelTest {
    @JvmField
    @RegisterExtension
    val main = MainDispatcher()

    @Test
    fun readsFirstLaunchOnce() {
        val firstLaunch = MemoryFirstLaunch()
        val start = FirstLaunchViewModel(firstLaunch)
        assertEquals(true, start.firstLaunchState.value, "pierwsze uruchomienie")
        assertEquals(true, start.firstLaunchState.value, "pierwsze uruchomienie bez ponownego odczytu")
        assertEquals(1, firstLaunch.reads, "jeden odczyt")
    }

    @Test
    fun savesHelpSeen() {
        val firstLaunch = MemoryFirstLaunch()
        val start = FirstLaunchViewModel(firstLaunch)
        start.markHelpSeen()
        assertEquals(true, firstLaunch.isHelpSeen, "zapamiętany znacznik pomocy")
        assertEquals(false, FirstLaunchViewModel(firstLaunch).firstLaunchState.value, "koniec pierwszego uruchomienia")
    }

    @Test
    fun skipsHelpOnReadError() {
        val broken =
            object : FirstLaunch {
                override suspend fun isFirstLaunch(): Boolean = throw IOException("odczyt")

                override suspend fun markHelpSeen() = Unit
            }

        assertEquals(false, FirstLaunchViewModel(broken).firstLaunchState.value, "pomoc pominięta po błędzie")
    }

    @Test
    fun keepsWriteError() {
        val broken =
            object : FirstLaunch {
                override suspend fun isFirstLaunch(): Boolean = false

                override suspend fun markHelpSeen() = throw IOException("zapis")
            }

        FirstLaunchViewModel(broken).markHelpSeen()
    }

    @Test
    fun savesAfterClear() {
        val firstLaunch = MemoryFirstLaunch()
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
        assertEquals(true, firstLaunch.isHelpSeen, "zapis po zamknięciu modelu")
    }
}

private class MemoryFirstLaunch : FirstLaunch {
    var reads = 0
    var isHelpSeen = false

    override suspend fun isFirstLaunch(): Boolean {
        reads++
        return !isHelpSeen
    }

    override suspend fun markHelpSeen() {
        isHelpSeen = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private class MainDispatcher : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
