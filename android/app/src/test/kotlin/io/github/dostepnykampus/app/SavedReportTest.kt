/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.data.submissions.OwnStatuses
import io.github.dostepnykampus.core.data.submissions.OwnSubmissions
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submissions.StatusRefresh
import io.github.dostepnykampus.core.data.submissions.SubmissionQueue
import io.github.dostepnykampus.core.data.submissions.SubmissionServer
import io.github.dostepnykampus.core.data.submissions.SubmissionStore
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsActions
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsContent
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsViewModel
import io.github.dostepnykampus.feature.capture.report.ReportEdit
import io.github.dostepnykampus.feature.capture.report.ReportFloor
import io.github.dostepnykampus.feature.capture.report.ReportForm
import io.github.dostepnykampus.feature.capture.report.ReportFormViewModel
import io.github.dostepnykampus.feature.capture.report.ReportPlace
import io.github.dostepnykampus.feature.capture.report.ReportSaverModule
import io.github.dostepnykampus.feature.capture.report.ReportStage
import kotlinx.coroutines.flow.first
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
 * „Zapisz” na C2 aż do karty na C3: formularz, zapis przez kolejkę w bazie z grafu Hilt, historia i lista
 * „Moje zgłoszenia” ze statusem „W telefonie”.
 *
 * Wymagania: US7.1, US7.3, BR-7, NFR-5
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class SavedReportTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()

    @Inject
    lateinit var store: SubmissionStore

    @Inject
    lateinit var pending: PendingSubmissionDao

    private var sent = 0

    @Before
    fun inject() {
        hilt.inject()
    }

    /**
     * TB-1, uciążliwość 3, piętro i zdjęcie: zgłoszenie jest w kolejce i na liście „W telefonie”.
     * Scenariusz: pomiar-precyzyjny.feature: Kompletne zgłoszenie zapisuje się ze statusem „lokalne”
     */
    @Test
    fun us7_1_complete_saved_local() {
        save(ReportEdit.Type(BarrierType.STAIRS_AT_ENTRANCE), ReportEdit.Rating(Severity.BLOCKING))

        assertEquals("TB-1", stored().barrierType)
        showMine()
        compose.onNodeWithText("W telefonie").assertIsDisplayed()
    }

    /**
     * TB-29 z opisem: zgłoszenie z opisem jest w kolejce i na liście „W telefonie”.
     * Scenariusz: pomiar-precyzyjny.feature: Typ TB-29 „inna bariera” z opisem zapisuje się
     */
    @Test
    fun us7_3_other_described_saved() {
        save(
            ReportEdit.Type(BarrierType.OTHER_BARRIER),
            ReportEdit.Rating(Severity.ONLY_WITH_HELP),
            ReportEdit.Description(DESCRIPTION),
        )

        assertEquals("TB-29", stored().barrierType)
        assertEquals(DESCRIPTION, stored().description)
        showMine()
        compose.onNodeWithText("W telefonie").assertIsDisplayed()
    }

    private fun save(vararg edits: ReportEdit) {
        val form = ReportFormViewModel(saver = ReportSaverModule.reportSaver(queue()))
        form.open(ReportForm(place = A1))
        form.edit(ReportEdit.Floor(ReportFloor.Numbered(0, isMezzanine = false)))
        form.edit(ReportEdit.Photo(photo()))
        edits.forEach(form::edit)
        form.save()
        compose.waitUntil(WAIT_MS) {
            shadowOf(Looper.getMainLooper()).idle()
            form.uiState.value?.stage == ReportStage.SAVED
        }
        assertEquals(0, sent)
    }

    private fun stored() = runBlocking { pending.all() }.single().submission

    private fun showMine() {
        val list = runBlocking { DataModule.ownSubmissions(store).observe().first() }
        val viewModel =
            MySubmissionsViewModel(
                history = OwnSubmissions { flowOf(list) },
                statuses = OwnStatuses { StatusRefresh.OFFLINE },
                networkStatus = { flowOf(false) },
            )
        compose.setContent {
            val state by viewModel.state.collectAsState()
            DostepnyKampusTheme {
                MySubmissionsContent(
                    state = state,
                    actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onOpen = {}),
                )
            }
        }
    }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = store,
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
        )

    private fun photo(): PendingPhotoDraft {
        val file = File(app.cacheDir, "p-lokalne.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return PendingPhotoDraft(photoId = "p-lokalne", path = file.path)
    }
}

private const val WAIT_MS = 5_000L
private const val DESCRIPTION = "Na długim korytarzu nie ma gdzie usiąść i odpocząć"
private val A1 =
    ReportPlace(
        location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
        accuracyMeters = 0.8,
        buildingSymbol = "A-1",
        isManualCorrection = false,
    )
