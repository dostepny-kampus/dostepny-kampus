/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.server

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

/** Wynik klienta HTTP staje się odpowiedzią serwera bez typów sieci. Wymagania: NFR-10, US9.3 */
class ApiRepliesTest {
    @Test
    fun nfr10_success_is_ok() {
        assertEquals(ServerReply.Ok(TEXT.length), reply(ApiResult.Success(TEXT)))
    }

    @Test
    fun nfr10_not_found_is_missing() {
        assertEquals(ServerReply.Missing, reply(ApiResult.NotFound))
    }

    @Test
    fun nfr10_network_error_is_offline() {
        assertEquals(ServerReply.Offline, reply(ApiResult.NetworkError(IOException("brak sieci"))))
    }

    @Test
    fun nfr10_server_error_is_fault() {
        assertEquals(ServerReply.Fault(SERVER_ERROR), reply(ApiResult.ServerError(SERVER_ERROR)))
    }

    @Test
    fun us9_3_edit_keeps_saved_and_conflict() {
        assertEquals(
            listOf(ServerEdit.Saved(SUBMISSION), ServerEdit.Conflict(SUBMISSION)),
            listOf(SubmissionEdit.Saved(SUBMISSION), SubmissionEdit.Conflict(SUBMISSION)).map { it.toServerEdit() },
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
