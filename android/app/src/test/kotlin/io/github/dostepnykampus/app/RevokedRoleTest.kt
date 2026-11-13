/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import io.github.dostepnykampus.core.data.submissions.SubmissionStamps
import io.github.dostepnykampus.core.database.OwnSubmissionDao
import io.github.dostepnykampus.core.database.PendingEditDao
import io.github.dostepnykampus.core.database.PendingSubmissionDao
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsActions
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsContent
import io.github.dostepnykampus.feature.capture.mine.MySubmissionsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

/** Odmowa 403: kolejka na bazie z grafu Hilt, historia i ViewModel aż do karty C3. Wymagania: US9.5, US14.1, BR-26 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class RevokedRoleTest {
    @get:Rule(order = 0)
    val hilt = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = RuntimeEnvironment.getApplication()

    @Inject
    lateinit var pending: PendingSubmissionDao

    @Inject
    lateinit var history: OwnSubmissionDao

    @Inject
    lateinit var edits: PendingEditDao
    private var creates = 0
    private val opened = mutableListOf<String>()

    @Before
    fun inject() {
        hilt.inject()
    }

    /**
     * Dwa przebiegi wysyłki: pierwszy dostaje 403, drugi już nie pyta serwera; C3 pokazuje powód i „Otwórz”.
     * Listę z historii Room test czyta raz, bo zapytanie Room idzie w wątku, którego `waitForIdle` nie widzi.
     * Scenariusz: synchronizacja.feature: Odebrana rola zostawia zgłoszenie w telefonie ze statusem „Nie wysłano”
     */
    @Test
    fun us9_5_revoked_role() {
        val queue = queue()
        runBlocking {
            queue.save(precise(), photo())
            queue.sendAll()
            queue.sendAll()
        }
        assertEquals(1, creates)
        assertNotNull(runBlocking { edits.find(ID) })

        val list = runBlocking { DataModule.ownSubmissions(history).observe().first() }
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
                    actions = MySubmissionsActions(onBack = {}, onMeasure = {}, onOpen = { opened.add(it) }),
                )
            }
        }

        compose.onNodeWithText("Nie wysłano").assertIsDisplayed()
        compose.onNodeWithText(NO_PERMISSION).assertIsDisplayed()
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(listOf(ID), opened)
    }

    private fun queue(): SubmissionQueue =
        SubmissionQueue(
            store = pending,
            ioContext = EmptyCoroutineContext,
            server =
                SubmissionServer(
                    create = { _, _ ->
                        creates++
                        ApiResult.ServerError(code = FORBIDDEN)
                    },
                    putPhoto = { _, _, _ -> ApiResult.Success(Unit) },
                    update = { _, _, _ -> ApiResult.NotFound },
                ),
            stamps = SubmissionStamps(newId = { ID }),
            history = history,
        )

    private fun photo(): List<PendingPhotoDraft> {
        val file = File(app.cacheDir, "$PHOTO.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = PHOTO, path = file.path))
    }

    private fun precise(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(PHOTO),
            buildingSymbol = "C-13",
            barrierType = "TB-13",
            severity = 2,
        )
}

private const val ID = "s-403"
private const val PHOTO = "p-403"
private const val FORBIDDEN = 403
private const val NO_PERMISSION =
    "Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie."
