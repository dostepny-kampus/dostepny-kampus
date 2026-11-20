/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.own

import io.github.dostepnykampus.core.data.submission.sync.FakeHistory
import io.github.dostepnykampus.core.data.submission.sync.FakeStore
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Historia „Moje zgłoszenia” jest osobna dla każdego konta na telefonie.
 *
 * Wymagania: US8.1, US8.2, NFR-33
 */
class OwnHistoryAccountTest {
    private val history = FakeHistory()
    private val store = FakeStore()
    private val subjects = MutableStateFlow<String?>("konto-a")

    @Test
    fun keepsListWithTheOwner() =
        runBlocking {
            history.save(row(id = "a", owner = "konto-a", reason = "Schody"))
            history.save(row(id = "b", owner = "konto-b", reason = "Próg"))
            val list = ownSubmissionsOf(history, store, subjects)

            val first = list.observe().first().single()
            assertEquals("a", first.id, "konto A widzi swoje zgłoszenie")
            assertEquals("Schody", first.rejectionReason, "powód odrzucenia zostaje przy koncie A")

            subjects.value = null
            assertEquals(emptyList<String>(), list.observe().first().map { it.id }, "po wylogowaniu lista jest pusta")

            subjects.value = "konto-b"
            val second = list.observe().first().single()
            assertEquals("b", second.id, "konto B widzi tylko swoje zgłoszenie")
            assertEquals("Próg", second.rejectionReason, "konto B nie dostaje powodu konta A")

            subjects.value = "konto-a"
            val back = list.observe().first().single()
            assertEquals("a", back.id, "po powrocie właściciela jego zgłoszenie wraca")
            assertEquals("Schody", back.rejectionReason, "powód odrzucenia wraca razem z listą")
        }

    @Test
    fun keepsOtherAccountOnRefresh() =
        runBlocking {
            history.save(row(id = "a", owner = "konto-a", reason = "Schody"))

            val result =
                ownStatusesOf(
                    mine = {
                        ApiResult.Success(
                            listOf(
                                server("a").copy(rejectionReason = "Cudze"),
                                server("b").copy(rejectionReason = "Moje"),
                            ),
                        )
                    },
                    history = history,
                    subject = { "konto-b" },
                ).refresh()

            assertEquals(StatusRefresh.DONE, result, "odświeżenie konta B dochodzi do skutku")
            assertEquals("Schody", history.rows.getValue("a").rejectionReason, "powód konta A zostaje")
            assertEquals("konto-a", history.rows.getValue("a").ownerSub, "wiersz konta A nie zmienia właściciela")
            assertEquals("konto-b", history.rows.getValue("b").ownerSub, "nowe zgłoszenie należy do konta B")
            assertEquals("Moje", history.rows.getValue("b").rejectionReason, "powód z serwera trafia do konta B")
        }

    private fun row(id: String, owner: String, reason: String) =
        OwnSubmissionEntity(
            id = id,
            savedAtMillis = 1,
            kind = "PRECISE",
            state = OwnSubmissionState.SENT,
            serverStatus = "REJECTED",
            rejectionReason = reason,
            ownerSub = owner,
        )

    private fun server(id: String): Submission =
        Submission(
            id = id,
            version = 2,
            status = SubmissionStatus.REJECTED,
            createdAt = Instant.parse("2026-11-10T12:00:00Z"),
            content =
                SubmissionContent(
                    kind = SubmissionKind.PRECISE,
                    location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                    photoIds = listOf("p1"),
                    barrierType = "TB-5",
                    severity = 2,
                ),
        )
}
