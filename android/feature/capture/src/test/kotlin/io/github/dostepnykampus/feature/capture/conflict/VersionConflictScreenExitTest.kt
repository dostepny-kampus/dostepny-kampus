/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.conflict

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.submission.conflict.ConflictField
import io.github.dostepnykampus.core.data.submission.conflict.VersionConflict
import io.github.dostepnykampus.core.data.submission.conflict.VersionConflicts
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.capture.navigation.VersionConflictRoute
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * C4 w aplikacji: wybór albo wyjście bez wyboru wraca do C3.
 *
 * Wymagania: US9.3, BR-10
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class VersionConflictScreenExitTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val calls = mutableListOf<String>()
    private var conflict: VersionConflict? = conflictOf()
    private var closed = 0
    private val conflicts =
        object : VersionConflicts {
            override suspend fun find(id: String): VersionConflict? = conflict?.takeIf { it.id == id }

            override suspend fun keepMine(id: String) {
                calls += "moja $id"
            }

            override suspend fun takeServer(id: String) {
                calls += "serwer $id"
            }
        }

    /** „Zostaw moją wersję” wykonuje wybór i wraca do C3. Wymagania: US9.3, BR-10 */
    @Test
    fun keepMineReturns() {
        show()
        compose.onNodeWithText("Zostaw moją wersję").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("wybór własnej wersji", listOf("moja $ID"), calls)
        assertEquals("powrót do C3", 1, closed)
    }

    /**
     * „Zdecyduję później” wraca do C3 bez wyboru.
     * Scenariusz (częściowo): synchronizacja.feature: Odłożony wybór wersji zostawia zmianę w telefonie
     *
     * Wymagania: BR-10
     */
    @Test
    fun laterReturnsUntouched() {
        show()
        compose.onNodeWithText("Zdecyduję później").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals("bez wyboru wersji", emptyList<String>(), calls)
        assertEquals("powrót do C3", 1, closed)
    }

    /** „Wstecz” wraca do C3 bez wyboru. Wymagania: BR-10 */
    @Test
    fun backReturnsUntouched() {
        show()
        compose.onNodeWithText("Wstecz").performClick()
        assertEquals("bez wyboru wersji", emptyList<String>(), calls)
        assertEquals("powrót do C3", 1, closed)
    }

    /** Zgłoszenie bez konfliktu wraca od razu do C3. Wymagania: US9.3 */
    @Test
    fun missingReturns() {
        conflict = null
        show()
        compose.waitForIdle()
        assertEquals("powrót do C3", 1, closed)
    }

    private fun show() {
        val viewModel = VersionConflictViewModel(conflicts)
        compose.setContent {
            DostepnyKampusTheme {
                VersionConflictScreen(route = VersionConflictRoute(ID), onClose = { closed++ }, viewModel = viewModel)
            }
        }
    }

    private companion object {
        const val ID = "s-c4"

        fun conflictOf(): VersionConflict {
            val own =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                    photoIds = listOf("p1"),
                    severity = 2,
                )
            val server =
                Submission(
                    id = ID,
                    version = 7,
                    status = SubmissionStatus.SENT,
                    createdAt = Instant.parse("2026-11-20T10:00:00Z"),
                    content = own.copy(severity = 3),
                )
            return VersionConflict(id = ID, own = own, server = server, differences = listOf(ConflictField.SEVERITY))
        }
    }
}
