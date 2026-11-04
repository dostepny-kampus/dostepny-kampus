/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ACCEPTED
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.ID
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PRECISE
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.PROBLEM
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.REJECTED
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.precise
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.quick
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.rejected
import io.github.dostepnykampus.core.network.SubmissionServer.Companion.submission
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.Instant

/**
 * Ciała `NewSubmission` i `SubmissionUpdate`, odczyt `Submission` i konflikt `VersionConflictProblem` z kontraktu.
 * Wartość wyliczenia spoza kontraktu to błąd treści: [ApiResult.ServerError] z przyczyną, bez wyjątku
 * i bez wartości zastępczej.
 */
class SubmissionsContractTest {
    private val server = SubmissionServer()

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.stop()
    }

    @Test
    fun br9_post_contract_fields() {
        server.enqueue(code = 201, body = submission(REJECTED))
        server.blocking { create(ID, precise) }
        assertEquals(server.parse("""{"id":"$ID",$PRECISE}"""), server.sentBody())
    }

    @Test
    fun br10_put_contract_fields() {
        server.enqueue(code = 200, body = submission(REJECTED))
        server.blocking { update(id = ID, version = 2, content = precise) }
        assertEquals(server.parse("""{"version":2,$PRECISE}"""), server.sentBody())
    }

    @Test
    fun br9_absent_not_sent() {
        server.enqueue(code = 201, body = submission(REJECTED))
        server.enqueue(code = 200, body = submission(REJECTED))
        server.blocking { create(ID, quick) }
        server.blocking { update(id = ID, version = 1, content = quick) }
        assertEquals(QUICK_FIELDS + "id", server.sentBody().keys - FLAGS)
        assertEquals(QUICK_FIELDS + "version", server.sentBody().keys - FLAGS)
    }

    @Test
    fun us8_1_rejected_read() {
        server.enqueue(code = 200, body = submission(REJECTED))
        assertEquals(ApiResult.Success(rejected), server.blocking { create(ID, precise) })
    }

    @Test
    fun us8_1_accepted_read() {
        server.enqueue(code = 200, body = "[${submission(ACCEPTED)}]")
        val verified = Instant.parse("2026-11-04T09:30:00Z")
        val accepted = rejected.copy(status = SubmissionStatus.ACCEPTED, rejectionReason = null, verifiedAt = verified)
        assertEquals(ApiResult.Success(listOf(accepted)), server.blocking { accepted("C-13") })
    }

    @Test
    fun us9_3_conflict_server() {
        server.enqueue(code = 409, body = """{$PROBLEM,"serverSubmission":${submission(REJECTED)}}""")
        val result = server.blocking { update(id = ID, version = 1, content = precise) }
        assertEquals(ApiResult.Success(SubmissionEdit.Conflict(rejected)), result)
    }

    @Test
    fun us9_3_conflict_no_server() {
        listOf("{$PROBLEM}", """{$PROBLEM,"serverSubmission":null}""", "").forEach { body ->
            server.enqueue(code = 409, body = body)
            val result = assertDoesNotThrow(body) { server.blocking { update(id = ID, version = 1, content = quick) } }
            assertContentError(409, result)
        }
    }

    @Test
    fun us8_1_unknown_enum_error() {
        UNKNOWN_VALUES.forEach { (known, unknown) ->
            server.enqueue(code = 201, body = submission(REJECTED).replace(oldValue = known, newValue = unknown))
            assertContentError(201, assertDoesNotThrow(unknown) { server.blocking { create(ID, precise) } })
        }
    }

    @Test
    fun us8_1_unknown_status_all_reads() {
        val body = submission(REJECTED).replace(oldValue = """"REJECTED"""", newValue = """"WITHDRAWN"""")
        server.enqueue(code = 200, body = body)
        server.enqueue(code = 409, body = """{$PROBLEM,"serverSubmission":$body}""")
        server.enqueue(code = 200, body = "[$body]")
        val edit = { server.blocking { update(id = ID, version = 1, content = precise) } }
        assertContentError(200, assertDoesNotThrow(edit))
        assertContentError(409, assertDoesNotThrow(edit))
        assertContentError(200, assertDoesNotThrow { server.blocking { accepted() } })
    }

    private fun assertContentError(code: Int, result: ApiResult<*>) {
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result)
        assertEquals(code, error.code)
        assertNotNull(error.cause, "Błąd treści bez przyczyny")
    }

    private companion object {
        val QUICK_FIELDS = setOf("kind", "location", "photoIds", "barrierGroup")
        val FLAGS = setOf("manualCorrection", "barrierRemoved")
        val UNKNOWN_VALUES =
            listOf(
                """"REJECTED"""" to """"WITHDRAWN"""",
                """"PRECISE"""" to """"PANORAMA"""",
                """"OTHER"""" to """"LIGHTING"""",
                """"INACCESSIBLE"""" to """"PARTIAL"""",
                """"INACCESSIBLE"""" to """"NO_DATA"""",
            )
    }
}
