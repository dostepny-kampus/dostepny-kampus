/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.startup.InitializationProvider
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Start aplikacji planuje wysyłkę kolejki zgłoszeń z poprzedniego procesu (NFR-6). */
@RunWith(RobolectricTestRunner::class)
class SubmissionSyncAtStartTest {
    private val app = RuntimeEnvironment.getApplication()

    @Before
    fun workManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(app)
    }

    @After
    fun closeWorkDatabase() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    // Wiersz: NFR-6, start aplikacji planuje wysyłkę, która czeka na internet.
    @Test
    fun nfr6_start_waits_for_network() {
        SubmissionSyncInitializer().create(app)
        val work = syncWork().single()
        assertEquals(WorkInfo.State.ENQUEUED, work.state)
        assertEquals(NetworkType.CONNECTED, work.constraints.requiredNetworkType)
    }

    // Wiersz: NFR-6, drugi start w tym samym oknie nie dubluje zadania.
    @Test
    fun nfr6_second_start_one_work() {
        SubmissionSyncInitializer().create(app)
        SubmissionSyncInitializer().create(app)
        assertEquals(1, syncWork().size)
    }

    // Wiersz: NFR-6, inicjalizator jest w manifeście, więc system woła go przy starcie procesu.
    @Test
    fun nfr6_initializer_in_manifest() {
        val provider =
            app.packageManager.getProviderInfo(
                ComponentName(app, InitializationProvider::class.java),
                PackageManager.GET_META_DATA,
            )
        assertEquals("androidx.startup", provider.metaData.getString(SubmissionSyncInitializer::class.java.name))
    }

    private fun syncWork(): List<WorkInfo> =
        WorkManager
            .getInstance(app)
            .getWorkInfosForUniqueWork(SYNC_WORK)
            .get()
}

// Nazwa zadania z `SubmissionSyncScheduler` w `core:data`.
private const val SYNC_WORK = "wysylka-zgloszen"
