/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.Manifest
import android.graphics.Bitmap
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.app.navigation.AppNavigator
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.capture.location.BuildingLookup
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.navigation.ReportStep
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.quick.QuickLookup
import io.github.dostepnykampus.feature.capture.quick.QuickReport
import io.github.dostepnykampus.feature.capture.quick.QuickReportActions
import io.github.dostepnykampus.feature.capture.quick.QuickReportModels
import io.github.dostepnykampus.feature.capture.quick.QuickReportScreen
import io.github.dostepnykampus.feature.capture.quick.QuickSendStage
import io.github.dostepnykampus.feature.capture.quick.QuickSendViewModel
import io.github.dostepnykampus.feature.capture.report.ReportSaverModule
import io.github.dostepnykampus.feature.map.navigation.BuildingRoute
import io.github.dostepnykampus.feature.map.navigation.MapRoute
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class QuickReturnPathTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()

    @Inject
    lateinit var store: SubmissionStore

    private val stack = mutableStateListOf<NavKey>(MapRoute())
    private val navigator =
        AppNavigator(backStack = stack, roles = TestRoles(Account.SignedIn()), restarts = mutableListOf())
    private val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { NO_BUILDINGS }
    private var created = 0
    private var photos = 0

    private val queue by lazy {
        SubmissionQueue(
            store = store,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    subject = { TEST_ACCOUNT },
                    create = { _, id, content ->
                        created++
                        accepted(id, content)
                    },
                    putPhoto = { _, _, _, _ ->
                        photos++
                        ServerReply.Ok(Unit)
                    },
                    update = { _, _, _, _ -> ServerReply.Missing },
                ),
        )
    }

    @Before
    fun inject() {
        hilt.inject()
    }

    /**
     * Po wysłaniu zgłoszenia szybkiego z karty A-1 „Wstecz” wraca na kartę, kolejne nie pokazuje kroków,
     * a serwer dostaje jedno zgłoszenie z jednym zdjęciem.
     * Scenariusz: zgloszenie-szybkie-powrot.feature: Po wysłaniu „Wstecz” wraca do ekranu, z którego zaczęto
     *   zgłoszenie
     *
     * Wymagania: US4.7
     */
    @Test
    fun returnsToCardAfterSend() {
        val photo = photoFile()
        navigator.openBuilding(symbol = CARD)
        navigator.report()
        navigator.nextReportStep(ReportRoute(photoId = PHOTO_ID, photoPath = photo.path))
        navigator.nextReportStep(top().copy(group = BarrierGroup.HORIZONTAL_CIRCULATION))
        assertEquals("krok wysyłki po grupie", ReportStep.SEND, top().step)
        val viewModel = sendModel(point = OUTSIDE)
        compose.setContent {
            val lookup by viewModel.lookup.collectAsState()
            Text(lookup.toString())
        }
        waitFor { viewModel.lookup.value is QuickLookup.Found }

        viewModel.send(QuickReport(photo = draft(photo), group = BarrierGroup.HORIZONTAL_CIRCULATION))
        waitFor { viewModel.stage.value == QuickSendStage.SENT }
        runBlocking { queue.sendAll() }
        navigator.reportSent()

        assertEquals("potwierdzenie po wysłaniu", ReportRoute(step = ReportStep.SENT), top())
        navigator.back()
        assertEquals("karta A-1 po pierwszym „Wstecz”", BuildingRoute(CARD), stack.last())
        navigator.back()
        assertEquals("po kolejnym „Wstecz” nie ma kroków", listOf<NavKey>(MapRoute()), stack.toList())
        assertEquals("jedno zgłoszenie na serwerze", 1, created)
        assertEquals("jedno zdjęcie w zgłoszeniu na serwerze", 1, photos)
    }

    /**
     * Krok 1 po powrocie z kroku 2 pokazuje zdjęcie, grupa „Toalety” zostaje wybrana po powrocie z kroku 3
     * i po ponownym „Dalej” z kroku 1, a „Wstecz” w kroku 1 usuwa zdjęcie dopiero po potwierdzeniu rezygnacji.
     * Scenariusz: zgloszenie-szybkie-powrot.feature: Powrót między krokami nie usuwa zdjęcia ani grupy barier
     *
     * Wymagania: US4.8
     */
    @Test
    fun keepsPhotoAndGroupAcrossSteps() {
        val photo = photoFile()
        navigator.openBuilding(symbol = CARD)
        navigator.report()
        navigator.nextReportStep(ReportRoute(photoId = PHOTO_ID, photoPath = photo.path))
        showReport()
        compose.onNodeWithText("Toalety").performClick()
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("✓ Toalety").assertIsSelected()
        compose.onNodeWithText("Wstecz").performClick()
        awaitPreview()
        assertTrue("zdjęcie zostaje w kroku 1", photo.exists())
        compose.onNodeWithText("Dalej").performClick()
        compose.onNodeWithText("✓ Toalety").assertIsSelected()
        compose.onNodeWithText("Wstecz").performClick()
        awaitPreview()
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Wróć do zdjęcia").performClick()
        assertTrue("zdjęcie zostaje do decyzji", photo.exists())
        compose.onNodeWithText("Wstecz").performClick()
        compose.onNodeWithText("Tak, zrezygnuj").performClick()
        assertFalse("zdjęcie usunięte dopiero po potwierdzeniu", photo.exists())
    }

    private fun showReport() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val models = QuickReportModels(photoTranscoder = PhotoTranscoder(), send = sendModel(point = OUTSIDE))
        compose.setContent {
            DostepnyKampusTheme {
                QuickReportScreen(route = top(), actions = actions(), models = models)
            }
        }
    }

    private fun actions() =
        QuickReportActions(
            onNext = { navigator.nextReportStep(it) },
            onSent = { navigator.reportSent(isSavedOffline = it) },
            onBack = { navigator.back() },
            onBackToMap = { navigator.backToMap() },
        )

    private fun awaitPreview() {
        val preview = hasContentDescription(PREVIEW)
        compose.waitUntil(timeoutMillis = WAIT_MS) { compose.onAllNodes(preview).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(preview).assertIsDisplayed()
    }

    private fun top(): ReportRoute = stack.last() as ReportRoute

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun sendModel(point: GeoPoint): QuickSendViewModel =
        QuickSendViewModel(
            positions = PositionSource { flowOf(PositionFix(point, ACCURACY, elapsedMillis = 0)) },
            locationSwitch = LocationSwitch { flowOf(true) },
            saver = ReportSaverModule.reportSaver(queue),
            networkStatus = NetworkStatus { flowOf(true) },
            buildingFloors = BuildingFloors { null },
            buildingLookup = BuildingLookup(catalog, EmptyCoroutineContext),
        ).apply { start(entranceId = null) }

    private fun accepted(id: String, content: SubmissionContent): ServerReply<Submission> =
        ServerReply.Ok(
            Submission(
                id = id,
                version = 1,
                status = SubmissionStatus.SENT,
                createdAt = Instant.EPOCH,
                content = content,
            ),
        )

    private fun photoFile(): File =
        File(app.cacheDir, "$PHOTO_ID.jpg").apply {
            outputStream().use { out ->
                val bitmap = Bitmap.createBitmap(BITMAP_SIDE, BITMAP_SIDE, Bitmap.Config.ARGB_8888)
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        }

    private fun draft(file: File) = PendingPhotoDraft(photoId = PHOTO_ID, path = file.path)
}

private const val CARD = "A-1"
private const val PHOTO_ID = "zdjecie-1"
private const val PREVIEW = "Zrobione zdjęcie 1 z 1"
private const val ACCURACY = 6.0
private const val WAIT_MS = 5_000L
private const val JPEG_QUALITY = 90
private const val BITMAP_SIDE = 4
private const val NO_BUILDINGS = """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[]}"""
private val OUTSIDE = GeoPoint(longitude = 17.059, latitude = 51.109)
