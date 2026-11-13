/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.BuildingFloors
import io.github.dostepnykampus.core.data.network.NetworkStatus
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue
import io.github.dostepnykampus.core.data.submissions.SubmissionServer
import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.location.LocationSwitch
import io.github.dostepnykampus.core.location.PositionFix
import io.github.dostepnykampus.core.location.PositionSource
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.feature.capture.BuildingLookup
import io.github.dostepnykampus.feature.capture.QuickLookup
import io.github.dostepnykampus.feature.capture.QuickReport
import io.github.dostepnykampus.feature.capture.QuickSendStage
import io.github.dostepnykampus.feature.capture.QuickSendViewModel
import io.github.dostepnykampus.feature.capture.ReportSaverModule
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

/**
 * „Wyślij” w kroku 3 V4 bez sieci: model widoku, zapis aplikacji i kolejka na bazie z grafu Hilt.
 *
 * Wymagania: US4.2, BR-9
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class QuickOfflineTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()

    @Inject
    lateinit var pending: PendingSubmissionDao

    @Inject
    lateinit var history: OwnSubmissionDao
    private var sent = 0
    private val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { NO_BUILDINGS }

    @Before
    fun inject() {
        hilt.inject()
    }

    /**
     * Bez sieci „Wyślij” kończy się stanem „zapisane w telefonie”, a zgłoszenie szybkie czeka w kolejce.
     * Scenariusz: zgloszenie-szybkie.feature: Zgłoszenie szybkie bez sieci zapisuje się w telefonie
     */
    @Test
    fun us4_2_offline_saved_in_phone() {
        val viewModel = viewModel()
        compose.setContent {
            val lookup by viewModel.lookup.collectAsState()
            Text(lookup.toString())
        }
        waitFor { viewModel.lookup.value is QuickLookup.Found }

        viewModel.send(QuickReport(photo = photo(), group = BarrierGroup.HORIZONTAL_CIRCULATION))
        waitFor { viewModel.stage.value == QuickSendStage.SAVED_OFFLINE }

        val stored = runBlocking { pending.all() }.single().submission
        assertEquals(SubmissionKind.QUICK.name, stored.kind)
        assertEquals(BarrierGroup.HORIZONTAL_CIRCULATION.name, stored.barrierGroup)
        assertEquals(0, sent)
    }

    private fun waitFor(condition: () -> Boolean) {
        compose.waitUntil(WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            condition()
        }
    }

    private fun viewModel(): QuickSendViewModel =
        QuickSendViewModel(
            positions = PositionSource { flowOf(PositionFix(STREET, ACCURACY, elapsedMillis = 0)) },
            locationSwitch = LocationSwitch { flowOf(true) },
            saver = ReportSaverModule.reportSaver(queue()),
            networkStatus = NetworkStatus { flowOf(false) },
            buildingFloors = BuildingFloors { null },
            buildingLookup = BuildingLookup(catalog, EmptyCoroutineContext),
        )

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = pending,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { _, _ ->
                        sent++
                        ApiResult.NetworkError(IOException("bez sieci"))
                    },
                    putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                    update = { _, _, _ -> ApiResult.NotFound },
                ),
            history = history,
        )

    private fun photo(): PendingPhotoDraft {
        val file = File(app.cacheDir, "p-szybkie.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return PendingPhotoDraft(photoId = "p-szybkie", path = file.path)
    }
}

private const val WAIT_MS = 5_000L
private const val ACCURACY = 6.0
private const val NO_BUILDINGS = """{"type":"FeatureCollection","attribution":"© OpenStreetMap","features":[]}"""
private val STREET = GeoPoint(longitude = 17.10, latitude = 51.20)
