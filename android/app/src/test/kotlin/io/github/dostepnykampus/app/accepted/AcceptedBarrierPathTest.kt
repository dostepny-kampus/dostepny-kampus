/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.accepted

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.content.FileProvider
import androidx.core.content.IntentCompat
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.app.EXPAND_HANDLE
import io.github.dostepnykampus.app.MainActivity
import io.github.dostepnykampus.app.TEST_ACCOUNT
import io.github.dostepnykampus.app.TestMapListChoice
import io.github.dostepnykampus.app.sentSubmission
import io.github.dostepnykampus.core.data.account.AccountSession
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.data.barrier.AcceptedBarriers
import io.github.dostepnykampus.core.data.building.BUILDINGS_ASSET
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.di.BarrierDataModule
import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.start.FirstLaunch
import io.github.dostepnykampus.core.data.submission.own.OwnStatuses
import io.github.dostepnykampus.core.data.submission.own.OwnSubmissions
import io.github.dostepnykampus.core.data.submission.own.StatusRefresh
import io.github.dostepnykampus.core.data.submission.pending.PendingEdits
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.SubmissionSyncScheduler
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.feature.capture.photo.photoDir
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import java.nio.file.Path
import java.time.Instant
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Pełna droga zmiany i usunięcia przyjętej bariery w grafie Hilt na ekranach aplikacji.
 *
 * Wymagania: BR-3, BR-18, US8.4, BR-19
 */
@HiltAndroidTest
@UninstallModules(DataModule::class, BarrierDataModule::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = HiltTestApplication::class,
    qualifiers = "w360dp-h800dp",
    instrumentedPackages = ["androidx.core.content"],
    shadows = [AcceptedBarrierPathTest.FileProviderPathShadow::class],
)
class AcceptedBarrierPathTest {
    /** FileProvider zakłada separator Androida, a Robolectric używa ścieżek systemu hosta. */
    @Implements(className = "androidx.core.content.FileProvider\$SimplePathStrategy", isInAndroidSdk = false)
    class FileProviderPathShadow {
        @Implementation
        fun belongsToRoot(filePath: String, rootPath: String): Boolean =
            Path.of(filePath).let { it != Path.of(rootPath) && it.startsWith(Path.of(rootPath)) }
    }

    /** Źródła danych i współdzielony stan dla pełnej ścieżki zmiany i usunięcia bariery. */
    @Module
    @InstallIn(SingletonComponent::class)
    object Fakes {
        val network = MutableStateFlow(false)
        var accepted: List<AcceptedBarrier> = emptyList()
        var scheduleSync = false
        val created = mutableListOf<SubmissionContent>()
        var uploadedPhotos = 0

        @Provides
        @Singleton
        fun catalog(app: Application): BuildingCatalogRepository =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                app.assets.open(BUILDINGS_ASSET).use { it.bufferedReader().readText() }
            }

        @Provides
        fun serverEntrances(): ServerEntrances = ServerEntrances { emptyList() }

        @Provides
        fun buildingFloors(): BuildingFloors = BuildingFloors { null }

        @Provides
        @Singleton
        fun mapChoice(): MapListChoice = TestMapListChoice().apply { savedChoice.value = true }

        @Provides
        fun network(): NetworkStatus = NetworkStatus { network }

        @Provides
        fun firstLaunch(): FirstLaunch =
            object : FirstLaunch {
                override suspend fun isFirstLaunch(): Boolean = false

                override suspend fun markHelpSeen() = Unit
            }

        @Provides
        fun ownSubmissions(store: SubmissionStore, tokens: AccessTokens): OwnSubmissions =
            DataModule.ownSubmissions(store, tokens)

        @Provides
        fun pendingEdits(store: SubmissionStore, tokens: AccessTokens): PendingEdits =
            DataModule.pendingEdits(store, tokens)

        @Provides
        fun ownStatuses(): OwnStatuses =
            OwnStatuses { if (network.value) StatusRefresh.DONE else StatusRefresh.OFFLINE }

        @Provides
        fun sync(app: Application): SubmissionSyncScheduler = SubmissionSyncScheduler(app)

        @Provides
        @Singleton
        fun queue(store: SubmissionStore, sync: SubmissionSyncScheduler, tokens: AccessTokens): SubmissionQueue {
            fun <T> send(reply: () -> ServerReply<T>) = if (network.value) reply() else ServerReply.Offline
            return SubmissionQueue(
                store = store,
                ioContext = EmptyCoroutineContext,
                server =
                    SubmissionServer(
                        subject = tokens::subject,
                        create = { _, id, c -> send { sentSubmission(id, c).also { created += c } } },
                        putPhoto = { _, _, _, _ -> send { ServerReply.Ok(Unit).also { uploadedPhotos += 1 } } },
                        update = { _, _, _, _ -> ServerReply.Missing },
                    ),
                onSaved = { if (scheduleSync) sync.scheduleAfterSave() },
            )
        }

        @Provides
        fun barriers(): AcceptedBarriers =
            object : AcceptedBarriers {
                override suspend fun barriers(): List<AcceptedBarrier> = accepted

                override fun syncedAt(): Instant = SYNCED_AT

                override fun known(): List<AcceptedBarrier> = accepted
            }
    }

    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private val app = RuntimeEnvironment.getApplication()

    @Inject lateinit var session: AccountSession

    @Inject lateinit var queue: SubmissionQueue

    @Inject lateinit var history: OwnSubmissionDao

    @Inject lateinit var pending: PendingSubmissionDao

    @Before
    fun prepareEnvironment() {
        Fakes.network.value = false
        Fakes.accepted = emptyList()
        Fakes.scheduleSync = false
        Fakes.created.clear()
        Fakes.uploadedPhotos = 0
        val cache = FileProvider::class.java.getDeclaredField("sCache").apply { isAccessible = true }
        (cache.get(null) as MutableMap<*, *>).clear()
        hilt.inject()
        val claims = """{"sub":"$TEST_ACCOUNT","realm_access":{"roles":["COLLECTOR"]}}"""
        val token = "e30.${Base64.getUrlEncoder().withoutPadding().encodeToString(claims.toByteArray())}.e30"
        runBlocking { session.signIn(token) }
        val factory =
            object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): Worker =
                    object : Worker(appContext, workerParameters) {
                        override fun doWork(): Result =
                            if (runBlocking { queue.sendAll() }) Result.success() else Result.retry()
                    }
            }
        val builder = Configuration.Builder()
        builder.setExecutor(SynchronousExecutor())
        builder.setWorkerFactory(factory)
        val config = builder.build()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, config)
        Fakes.scheduleSync = true
    }

    @After
    fun closeWorkDatabase() = WorkManagerTestInitHelper.closeWorkDatabase()

    /**
     * Zmiana przyjętego progu przez ekran zmiany tworzy zgłoszenie lokalne obok przyjętego na liście i karcie.
     * Scenariusz: zgloszenia-zbierajacego.feature: Zmiana zgłoszenia przyjętego tworzy nowe zgłoszenie
     *
     * Wymagania: BR-3, BR-18
     */
    @Test
    fun replacesAcceptedThreshold() {
        runBlocking {
            history.save(
                OwnSubmissionEntity(
                    id = THRESHOLD_ID,
                    savedAtMillis = SYNCED_AT.toEpochMilli(),
                    kind = SubmissionKind.PRECISE.name,
                    buildingSymbol = BUILDING,
                    floorNumber = 0,
                    barrierType = "TB-13",
                    severity = Severity.ONLY_WITH_HELP.value,
                    state = OwnSubmissionState.SENT,
                    serverVersion = 2,
                    serverStatus = SubmissionStatus.ACCEPTED.name,
                    ownerSub = TEST_ACCOUNT,
                ),
            )
        }
        Fakes.accepted = listOf(barrier(THRESHOLD_ID, BarrierType.THRESHOLD, Severity.ONLY_WITH_HELP, floor = 0))
        openCard()
        compose.waitUntil(WAIT_MS) { hasText(REPLACE_BUTTON) }
        compose.onNodeWithText(REPLACE_BUTTON).performScrollTo().performClick()
        compose.waitUntil(WAIT_MS) { hasText("Zapis utworzy nowe zgłoszenie, które znów sprawdzi Weryfikator.") }
        compose.onNodeWithText("Blokada").performScrollTo().performClick()
        takeCameraPhoto()
        compose.onNodeWithText("Zapisz").performClick()
        awaitOwnList("W telefonie", "Przyjęte")
        val queued = runBlocking { pending.all() }.single().submission
        assertEquals("nowe zgłoszenie wskazuje poprzednie przyjęte", THRESHOLD_ID, queued.replacesSubmissionId)
        assertEquals("nowe zgłoszenie ma zmienioną uciążliwość", Severity.BLOCKING.value, queued.severity)
        assertTrue("bez sieci zmiana nie trafia na serwer", Fakes.created.isEmpty())
        assertEquals("zmiana tworzy wpis", 2, runBlocking { history.observe(TEST_ACCOUNT).first() }.size)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        expandBarriers()
        compose.onNodeWithText("Próg").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Tylko z pomocą").performScrollTo().assertIsDisplayed()
    }

    /**
     * Inny Zbierający zgłasza usunięcie windy ze zdjęciem w sieci i widzi ją nadal na karcie C-13.
     * Scenariusz: zgloszenia-zbierajacego.feature: Inny Zbierający niż autor zgłasza usunięcie bariery
     *
     * Wymagania: US8.4, BR-19
     */
    @Test
    fun reportsLiftRemoval() {
        Fakes.network.value = true
        Fakes.accepted = listOf(barrier("s-9", BarrierType.BROKEN_LIFT, Severity.BLOCKING, floor = 2))
        openCard()
        compose.onNodeWithText(LIFT_NAME).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Zgłoś, że bariery nie ma").performScrollTo().performClick()
        compose.waitUntil(WAIT_MS) { hasText("Zgłoś, że bariery nie ma") }
        takeCameraPhoto()
        compose.onNodeWithText("Wyślij do sprawdzenia").performClick()
        compose.waitUntil(WAIT_MS) { hasText("Moje zgłoszenia") }
        val infos = WorkManager.getInstance(app).getWorkInfosForUniqueWork("wysylka-zgloszen")
        val workId = infos.get().single().id
        WorkManagerTestInitHelper.getTestDriver(app)?.setAllConstraintsMet(workId)
            ?: error("brak sterownika WorkManagera")
        awaitOwnList("Wysłane")
        assertTrue("kolejka pusta po wysłaniu usunięcia", runBlocking { pending.all() }.isEmpty())
        val sent = Fakes.created.single()
        assertEquals("oznaczenie usunięcia bariery", true, sent.isBarrierRemoved)
        assertEquals("powiązanie z poprzednim zgłoszeniem windy", "s-9", sent.replacesSubmissionId)
        assertEquals("wysłane jedno nowe zdjęcie sprawnej windy", 1, Fakes.uploadedPhotos)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        expandBarriers()
        compose.onNodeWithText(LIFT_NAME).performScrollTo().assertIsDisplayed()
    }

    private fun openCard() {
        compose.waitUntil(WAIT_MS) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performTextInput(BUILDING)
        compose.waitUntil(WAIT_MS) { compose.onAllNodes(listedC13).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(listedC13).onFirst().performClick()
        expandBarriers()
    }

    private fun expandBarriers() {
        compose.waitUntil(WAIT_MS) { hasText("Zamknij") }
        if (!hasText("Bariery w budynku")) {
            compose.onNodeWithContentDescription(EXPAND_HANDLE).performClick()
        }
        compose.waitUntil(WAIT_MS) { hasText("Bariery w budynku") }
    }

    private fun takeCameraPhoto() {
        compose.onNodeWithText("Zrób zdjęcie").performScrollTo().performClick()
        val request = checkNotNull(shadowOf(compose.activity).nextStartedActivityForResult) { "brak żądania aparatu" }
        assertEquals("przycisk zdjęcia otwiera aparat", MediaStore.ACTION_IMAGE_CAPTURE, request.intent.action)
        val output = IntentCompat.getParcelableExtra(request.intent, MediaStore.EXTRA_OUTPUT, Uri::class.java)
        val file = photoDir(compose.activity.filesDir).resolve(output?.lastPathSegment.orEmpty())
        val bitmap = Bitmap.createBitmap(PHOTO_SIDE, PHOTO_SIDE, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        shadowOf(compose.activity).receiveResult(request.intent, Activity.RESULT_OK, null)
        compose.waitUntil(WAIT_MS) { hasText("Zrób nowe zdjęcie") }
    }

    private fun awaitOwnList(vararg texts: String) {
        compose.waitUntil(WAIT_MS) { texts.all(::hasText) }
        compose.onNodeWithText(texts.first()).assertIsDisplayed()
    }

    private fun hasText(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun barrier(id: String, type: BarrierType, severity: Severity, floor: Int) =
        AcceptedBarrier(
            id = id,
            location = LOCATION,
            severity = severity,
            type = type,
            buildingSymbol = BUILDING,
            verifiedAt = SYNCED_AT,
            floor = SubmissionFloor(number = floor),
        )

    private companion object {
        const val WAIT_MS = 5_000L
        const val PHOTO_SIDE = 4
        const val JPEG_QUALITY = 90
        const val BUILDING = "C-13"
        const val THRESHOLD_ID = "s-0"
        const val LIFT_NAME = "Niesprawna winda"
        const val REPLACE_BUTTON = "Popraw zgłoszenie"
        val listedC13 = hasContentDescription("C-13", substring = true)
        val LOCATION = GeoPoint(longitude = 17.05, latitude = 51.125)
        val SYNCED_AT: Instant = Instant.parse("2026-11-12T10:00:00Z")
    }
}
