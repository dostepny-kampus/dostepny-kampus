/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Zgłoszenia na MockWebServer: wysłanie, ponowienie, edycja z konfliktem wersji, zdjęcie i lista przyjętych. */
class SubmissionsApiTest {
    private val server = MockWebServer()
    private val json = Json { ignoreUnknownKeys = true }
    private val quick =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.0, latitude = 51.0),
            photoIds = listOf(PHOTO),
            barrierGroup = BarrierGroup.OTHER,
        )

    @BeforeEach
    fun start() {
        server.start()
    }

    @AfterEach
    fun stop() {
        server.shutdown()
    }

    @Test
    fun br9_create_and_retry() {
        enqueue(code = 201, body = submission(version = 1))
        enqueue(code = 200, body = submission(version = 1))
        assertEquals(blocking { create(ID, quick) }, blocking { create(ID, quick) })
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/submissions", recorded.path)
        assertTrue(recorded.body.readUtf8().contains(""""id":"$ID""""))
    }

    @Test
    fun br10_update_saved() {
        enqueue(code = 200, body = submission(version = 2))
        val saved = blocking { update(id = ID, version = 1, content = quick) }
        val edit = assertInstanceOf(ApiResult.Success::class.java, saved).value
        assertEquals(2, assertInstanceOf(SubmissionEdit.Saved::class.java, edit).submission.version)
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("/submissions/$ID", recorded.path)
    }

    @Test
    fun br10_update_conflict() {
        enqueue(code = 409, body = """{"status":409,"serverSubmission":${submission(version = 3)}}""")
        val result = blocking { update(id = ID, version = 1, content = quick) }
        val edit = assertInstanceOf(ApiResult.Success::class.java, result).value
        assertEquals(3, assertInstanceOf(SubmissionEdit.Conflict::class.java, edit).server.version)
    }

    @Test
    fun br10_conflict_without_server() {
        enqueue(code = 409, body = """{"status":409}""")
        val result = blocking { update(id = ID, version = 1, content = quick) }
        assertEquals(409, assertInstanceOf(ApiResult.ServerError::class.java, result).code)
    }

    @Test
    fun br7_rules_and_missing() {
        enqueue(code = 400, body = """{"status":400}""")
        assertEquals(ApiResult.ServerError(400), blocking { create(ID, quick) })
        enqueue(code = 404, body = """{"status":404}""")
        assertEquals(ApiResult.NotFound, blocking { putPhoto(submissionId = ID, photoId = PHOTO, jpeg = ByteArray(0)) })
    }

    @Test
    fun nfr26_photo_upload() {
        server.enqueue(MockResponse().setResponseCode(204))
        val jpeg = byteArrayOf(-1, -40)
        assertEquals(ApiResult.Success(Unit), blocking { putPhoto(submissionId = ID, photoId = PHOTO, jpeg = jpeg) })
        val recorded = server.takeRequest()
        assertEquals("/submissions/$ID/photos/$PHOTO", recorded.path)
        assertEquals("image/jpeg", recorded.getHeader("Content-Type"))
    }

    @Test
    fun us2_2_accepted_list() {
        enqueue(code = 200, body = "[${submission(version = 1)}]")
        val result = blocking { accepted("A-1") }
        assertEquals(ApiResult.Success::class, result::class)
        assertEquals(1, (result as ApiResult.Success).value.size)
        assertEquals("/submissions?buildingSymbol=A-1", server.takeRequest().path)
    }

    @Test
    fun us9_2_no_network() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertInstanceOf(ApiResult.NetworkError::class.java, blocking { create(ID, quick) })
    }

    private fun <T> blocking(call: suspend SubmissionsApi.() -> T): T =
        runBlocking { SubmissionsApi(OkHttpClient(), json, server.url("/").toString()).call() }

    private fun enqueue(code: Int, body: String) {
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))
    }

    private fun submission(version: Int): String {
        val sent = json.parseToJsonElement(json.encodeNewSubmission(id = ID, content = quick)).jsonObject
        val meta = """"version":$version,"status":"SENT","createdAt":"2026-11-03T18:00:00Z","""
        return sent.toString().replaceFirst(oldValue = "{", newValue = "{$meta")
    }

    private companion object {
        const val ID = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
        const val PHOTO = "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
    }
}
