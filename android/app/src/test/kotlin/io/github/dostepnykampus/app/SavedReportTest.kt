/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.NearestEntrance
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.database.submission.PendingSubmissionDao
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.capture.report.ReportFormViewModel
import io.github.dostepnykampus.feature.capture.report.ReportSaverModule
import io.github.dostepnykampus.feature.capture.report.form.ReportEdit
import io.github.dostepnykampus.feature.capture.report.form.ReportFloor
import io.github.dostepnykampus.feature.capture.report.form.ReportForm
import io.github.dostepnykampus.feature.capture.report.form.ReportPlace
import io.github.dostepnykampus.feature.capture.report.form.ReportStage
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
    fun savesCompleteLocally() {
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
    fun savesDescribedOther() {
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
        val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { NO_BUILDINGS }
        val nearest = NearestEntrance(catalog)
        val form = ReportFormViewModel(nearest = nearest, saver = ReportSaverModule.reportSaver(queue()))
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
        val viewModel = offlineOwnList(store)
        compose.setContent { OwnListScreen(viewModel) }
    }

    private fun queue(): SubmissionQueue = offlineQueue(store) { sent++ }

    private fun photo(): PendingPhotoDraft = photoDraft(app.cacheDir, "p-lokalne")
}

private const val WAIT_MS = 5_000L
private const val NO_BUILDINGS = """{"type":"FeatureCollection","attribution":"© plik","features":[]}"""
private const val DESCRIPTION = "Na długim korytarzu nie ma gdzie usiąść i odpocząć"
private val A1 =
    ReportPlace(
        location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
        accuracyMeters = 0.8,
        buildingSymbol = "A-1",
        isManualCorrection = false,
    )
