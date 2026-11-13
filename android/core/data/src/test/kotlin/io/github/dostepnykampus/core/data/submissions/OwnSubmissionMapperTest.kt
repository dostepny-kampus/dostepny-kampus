/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionState
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.SubmissionEdit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/** Kod odmowy i wersja z odpowiedzi serwera dla historii C3. Wymagania: US9.5, BR-10 */
class OwnSubmissionMapperTest {
    @Test
    fun `us9_5 kod odmowy`() {
        val codes =
            listOf(
                ApiResult.ServerError(code = 422),
                ApiResult.NotFound,
                ApiResult.Success(SubmissionEdit.Conflict(submission)),
                ApiResult.Success(SubmissionEdit.Saved(submission)),
                ApiResult.NetworkError(IOException("sieć")),
            ).map { refusalCodeOf(it) }

        assertEquals(listOf(422, 404, 409, null, null), codes)
    }

    /** 403 to brak uprawnień, 409 konflikt wersji, inne kody to nieprzyjęte, brak kodu to nieodczytane. */
    @Test
    fun `us9_5 powód z kodu`() {
        val reasons = listOf(403, 409, 400, 404, 413, 422, null).map { refusalReasonOf(OwnSubmissionState.REFUSED, it) }

        val others = List(4) { RefusalReason.NOT_ACCEPTED }
        val expected = listOf(RefusalReason.NO_PERMISSION, RefusalReason.CHANGED_ON_SERVER) + others
        assertEquals(expected + RefusalReason.UNREADABLE, reasons)
        assertEquals(null, refusalReasonOf(OwnSubmissionState.SENT, 403))
    }

    @Test
    fun `br10 wersja z odpowiedzi`() {
        val versions =
            listOf(
                ApiResult.Success(submission),
                ApiResult.Success(SubmissionEdit.Saved(submission)),
                ApiResult.Success(SubmissionEdit.Conflict(submission)),
                ApiResult.NotFound,
            ).map { versionOf(it) }

        assertEquals(listOf(7, 7, null, null), versions)
    }
}

private val submission =
    Submission(
        id = "s1",
        version = 7,
        status = SubmissionStatus.SENT,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        content =
            SubmissionContent(
                kind = SubmissionKind.PRECISE,
                location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                photoIds = listOf("p1"),
            ),
    )
