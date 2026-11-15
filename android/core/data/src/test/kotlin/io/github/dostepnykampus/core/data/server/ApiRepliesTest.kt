/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.server

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.submission.SubmissionEdit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/** Wynik klienta HTTP staje się odpowiedzią serwera bez typów sieci. Wymagania: NFR-10, US9.3 */
class ApiRepliesTest {
    @Test
    fun mapsSuccessToOk() {
        assertEquals(ServerReply.Ok(TEXT.length), reply(ApiResult.Success(TEXT)), "sukces daje odpowiedź z treścią")
    }

    @Test
    fun mapsNotFoundToMissing() {
        assertEquals(ServerReply.Missing, reply(ApiResult.NotFound), "nie znaleziono daje brak zasobu")
    }

    @Test
    fun mapsNetworkErrorToOffline() {
        assertEquals(
            ServerReply.Offline,
            reply(ApiResult.NetworkError(IOException("brak sieci"))),
            "błąd sieci daje offline",
        )
    }

    @Test
    fun mapsServerErrorToFault() {
        assertEquals(
            ServerReply.Fault(SERVER_ERROR),
            reply(ApiResult.ServerError(SERVER_ERROR)),
            "błąd serwera daje usterkę",
        )
    }

    @Test
    fun keepsVariantOnEdit() {
        assertEquals(
            listOf(ServerEdit.Saved(SUBMISSION), ServerEdit.Conflict(SUBMISSION)),
            listOf(SubmissionEdit.Saved(SUBMISSION), SubmissionEdit.Conflict(SUBMISSION)).map { it.toServerEdit() },
            "zapis i konflikt przekładane na odpowiedzi serwera",
        )
    }

    private fun reply(result: ApiResult<String>): ServerReply<Int> = result.toReply { it.length }

    private companion object {
        const val TEXT = "karta"
        const val SERVER_ERROR = 503
        val SUBMISSION =
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
    }
}
