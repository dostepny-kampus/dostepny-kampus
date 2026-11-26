/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.quick

import android.content.Context
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.app.photoDraft
import io.github.dostepnykampus.app.queueWith
import io.github.dostepnykampus.app.sentSubmission
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.feature.capture.quick.QuickLookup
import io.github.dostepnykampus.feature.capture.quick.QuickReport
import io.github.dostepnykampus.feature.capture.quick.QuickSendStage
import io.github.dostepnykampus.feature.capture.report.ReportSaverModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import javax.inject.Inject

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class QuickNetworkReturnTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()
    private val network = MutableStateFlow(false)
    private val created = mutableListOf<String>()

    @Inject
    lateinit var store: SubmissionStore

    @Inject
    lateinit var pending: PendingSubmissionDao

    @Before
    fun inject() {
        hilt.inject()
    }

    @After
    fun closeWorkDatabase() {
        WorkManagerTestInitHelper.closeWorkDatabase()
    }

    /**
     * Zgłoszenie zapisane bez sieci czeka w kolejce, a po powrocie sieci zadanie wysyłki samo wysyła je do
     * serwera jeden raz, bez żadnego działania Odwiedzającego.
     * Scenariusz: zgloszenie-szybkie.feature: Zgłoszenie szybkie zapisane bez sieci wysyła się samo po powrocie
     *   sieci
     *
     * Wymagania: US4.2, BR-9
     */
    @Test
    fun sendsSavedReportAfterNetwork() {
        val scheduler = SubmissionSyncScheduler(app)
        val queue =
            queueWith(store, onSaved = scheduler::scheduleAfterSave) { id, content ->
                if (network.value) sentSubmission(id, content).also { created += id } else ServerReply.Offline
            }
        WorkManagerTestInitHelper.initializeTestWorkManager(app, syncConfiguration(queue))
        val where = app.quickSendModel(INSIDE_D2, NetworkStatus { network }, ReportSaverModule.reportSaver(queue))
        compose.setContent {
            val lookup = where.lookup.collectAsState()
            Text(lookup.value.toString())
        }
        waitFor { where.lookup.value is QuickLookup.Found }
        val photo = photoDraft(app.cacheDir, "p-szybkie")
        where.send(QuickReport(photo = photo, group = BarrierGroup.HORIZONTAL_CIRCULATION))
        waitFor { where.stage.value == QuickSendStage.SAVED_OFFLINE }

        val manager = WorkManager.getInstance(app)
        val waiting = manager.getWorkInfosForUniqueWork(SYNC_WORK).get().single()
        assertEquals("zgłoszenie czeka w telefonie", 1, runBlocking { pending.all() }.size)
        assertEquals("wysyłka czeka na sieć", WorkInfo.State.ENQUEUED, waiting.state)
        assertTrue("serwer nic nie dostał bez sieci", created.isEmpty())

        network.value = true
        WorkManagerTestInitHelper.getTestDriver(app)?.setAllConstraintsMet(waiting.id)

        val work = manager.getWorkInfoById(waiting.id).get()
        assertEquals("zadanie wysyłki skończone po powrocie sieci", WorkInfo.State.SUCCEEDED, work?.state)
        assertEquals("na serwerze jedno zgłoszenie", 1, created.size)
        assertTrue("kolejka w telefonie pusta", runBlocking { pending.all() }.isEmpty())
    }

    private fun syncConfiguration(queue: SubmissionQueue): Configuration =
        Configuration
            .Builder()
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(
                object : WorkerFactory() {
                    override fun createWorker(
                        appContext: Context,
                        workerClassName: String,
                        workerParameters: WorkerParameters,
                    ): ListenableWorker = QueueWorker(appContext, workerParameters, queue)
                },
            ).build()

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }
}

private class QueueWorker(context: Context, params: WorkerParameters, private val queue: SubmissionQueue) :
    Worker(context, params) {
    override fun doWork(): Result = if (runBlocking { queue.sendAll() }) Result.success() else Result.retry()
}

private const val WAIT_MS = 10_000L
private const val SYNC_WORK = "wysylka-zgloszen"
