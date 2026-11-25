/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.network.submission.SubmissionEdit
import io.github.dostepnykampus.core.network.submission.SubmissionsApi
import io.github.dostepnykampus.core.network.submission.encodeNewSubmission
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Zgłoszenia na MockWebServer: wysłanie, ponowienie, edycja z konfliktem wersji, zdjęcie i lista przyjętych.
 *
 * Wymagania: BR-9, BR-10, BR-7, NFR-26, US9.2
 */
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
    fun createsAndRetries() {
        enqueue(code = 201, body = submission(version = 1))
        enqueue(code = 200, body = submission(version = 1))
        assertEquals(
            blocking { create(ID, quick) },
            blocking { create(ID, quick) },
            "Ponowienie zwraca to samo zgłoszenie",
        )
        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method, "Utworzenie to POST")
        assertEquals("/submissions", recorded.path, "Ścieżka utworzenia")
        assertTrue(recorded.body.readUtf8().contains(""""id":"$ID""""), "Treść zawiera identyfikator zgłoszenia")
    }

    @Test
    fun fetchesSubmissionById() {
        enqueue(code = 200, body = submission(version = 2))
        val result = blocking { submission(ID) }
        assertEquals(2, (result as ApiResult.Success).value.version, "Bieżąca wersja zgłoszenia")
        val request = server.takeRequest()
        assertEquals("GET", request.method, "Odczyt to GET")
        assertEquals("/submissions/$ID", request.path, "Ścieżka pojedynczego zgłoszenia")
    }

    @Test
    fun reportsMissingSubmission() {
        enqueue(code = 404, body = "")
        assertEquals(ApiResult.NotFound, blocking { submission(ID) }, "Brak zgłoszenia to NotFound")
    }

    @Test
    fun savesUpdate() {
        enqueue(code = 200, body = submission(version = 2))
        val saved = blocking { update(id = ID, version = 1, content = quick) }
        val edit = assertInstanceOf(ApiResult.Success::class.java, saved, "Zapis kończy się sukcesem").value
        val savedEdit = assertInstanceOf(SubmissionEdit.Saved::class.java, edit, "Edycja jest zapisana")
        assertEquals(2, savedEdit.submission.version, "Serwer zwiększa wersję do 2")
        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method, "Edycja to PUT")
        assertEquals("/submissions/$ID", recorded.path, "Ścieżka edycji")
    }

    @Test
    fun reportsUpdateConflict() {
        val problem = """{"title":"Konflikt wersji","status":409"""
        enqueue(code = 409, body = """$problem,"serverSubmission":${submission(version = 3)}}""")
        val result = blocking { update(id = ID, version = 1, content = quick) }
        val edit = assertInstanceOf(ApiResult.Success::class.java, result, "Konflikt wersji to wynik, nie błąd").value
        val conflict = assertInstanceOf(SubmissionEdit.Conflict::class.java, edit, "Edycja kończy się konfliktem")
        assertEquals(3, conflict.server.version, "Konflikt niesie wersję serwera 3")
    }

    @Test
    fun reportsConflictWithoutServer() {
        enqueue(code = 409, body = """{"status":409}""")
        val result = blocking { update(id = ID, version = 1, content = quick) }
        val error = assertInstanceOf(ApiResult.ServerError::class.java, result, "Konflikt bez zgłoszenia to błąd")
        assertEquals(409, error.code, "Kod błędu 409")
    }

    @Test
    fun reportsRulesAndMissing() {
        enqueue(code = 400, body = """{"status":400}""")
        assertEquals(ApiResult.ServerError(400), blocking { create(ID, quick) }, "Odrzucenie reguł to błąd 400")
        enqueue(code = 404, body = """{"status":404}""")
        val missing = blocking { putPhoto(submissionId = ID, photoId = PHOTO, jpeg = ByteArray(0)) }
        assertEquals(ApiResult.NotFound, missing, "Brak zgłoszenia to NotFound")
    }

    @Test
    fun uploadsPhoto() {
        server.enqueue(MockResponse().setResponseCode(204))
        val jpeg = byteArrayOf(-1, -40)
        val result = blocking { putPhoto(submissionId = ID, photoId = PHOTO, jpeg = jpeg) }
        assertEquals(ApiResult.Success(Unit), result, "Wysłanie zdjęcia kończy się sukcesem")
        val recorded = server.takeRequest()
        assertEquals("/submissions/$ID/photos/$PHOTO", recorded.path, "Ścieżka zdjęcia")
        assertEquals("image/jpeg", recorded.getHeader("Content-Type"), "Zdjęcie idzie jako JPEG")
    }

    /** Wiersz: arkusz bariery, zdjęcie zgłoszenia przyjętego jako JPEG. Wymagania: US1.5, US2.2 */
    @Test
    fun downloadsPhoto() {
        val content = "zdjecie-jpeg"
        val response = MockResponse().setResponseCode(200).setHeader(name = "Content-Type", value = "image/jpeg")
        server.enqueue(response.setBody(content))
        val jpeg = content.toByteArray()
        val result = blocking { photo(submissionId = ID, photoId = PHOTO) }
        assertArrayEquals(jpeg, (result as ApiResult.Success).value, "Pobrane bajty zdjęcia")
        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method, "Pobranie to GET")
        assertEquals("/submissions/$ID/photos/$PHOTO", recorded.path, "Ścieżka zdjęcia")
        assertTrue(recorded.getHeader("Accept").orEmpty().startsWith("image/jpeg"), "Żądanie prosi o JPEG")
    }

    /** Wiersz: arkusz bariery, zdjęcia nie ma na serwerze → brak zdjęcia, nie błąd. Wymagania: US1.5 */
    @Test
    fun reportsMissingPhoto() {
        enqueue(code = 404, body = """{"status":404}""")
        val result = blocking { photo(submissionId = ID, photoId = PHOTO) }
        assertEquals(ApiResult.NotFound, result, "Brak zdjęcia to NotFound")
    }

    @Test
    fun fetchesAcceptedList() {
        enqueue(code = 200, body = "[${submission(version = 1)}]")
        val result = blocking { accepted("A-1") }
        assertEquals(ApiResult.Success::class, result::class, "Lista przyjętych kończy się sukcesem")
        assertEquals(1, (result as ApiResult.Success).value.size, "Lista ma jedno zgłoszenie")
        assertEquals(
            "/submissions?status=ACCEPTED&buildingSymbol=A-1",
            server.takeRequest().path,
            "Ścieżka z filtrem statusu i budynku",
        )
    }

    /** Lista przyjętych z serwera stronami: klient idzie za `Link` z `rel="next"`. Wymagania: NFR-34, US1.3 */
    @Test
    fun followsNextPageLinks() {
        val next = server.url("/submissions?status=ACCEPTED&after=$ID")
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(name = "Link", value = "<$next>; rel=\"next\"")
                .setBody("[${submission(version = 1)}]"),
        )
        enqueue(code = 200, body = "[${submission(version = 2)}]")
        val result = blocking { accepted() }
        assertEquals(
            listOf(1, 2),
            (result as ApiResult.Success).value.map { it.version },
            "Lista nie złożyła obu stron w kolejności",
        )
        server.takeRequest()
        assertEquals(
            "/submissions?status=ACCEPTED&after=$ID",
            server.takeRequest().path,
            "Druga strona nie poszła pod adres z nagłówka Link",
        )
    }

    /** Błąd dalszej strony to błąd całej listy; kopia w telefonie zostaje bez zmian. Wymagania: NFR-34 */
    @Test
    fun laterPageErrorFailsList() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader(name = "Link", value = "</submissions?mine=true&after=$ID>; rel=\"next\"")
                .setBody("[${submission(version = 1)}]"),
        )
        enqueue(code = 500, body = "")
        assertEquals(ApiResult.ServerError(500), blocking { mine() }, "Błąd drugiej strony nie zatrzymał listy")
    }

    /** Wiersz: C3 → własne zgłoszenia z serwera w każdym statusie. Wymagania: US8.1, US8.2 */
    @Test
    fun fetchesOwnList() {
        enqueue(code = 200, body = "[${submission(version = 2)}]")
        val result = blocking { mine() }
        assertEquals(2, (result as ApiResult.Success).value.single().version, "Własne zgłoszenie ma wersję 2")
        assertEquals("/submissions?mine=true", server.takeRequest().path, "Ścieżka własnych zgłoszeń")
    }

    /** Wiersz: C3 bez roli Zbierającego → błąd z kodem, nie lista. Wymagania: US8.1 */
    @Test
    fun reportsOwnListForbidden() {
        enqueue(code = 403, body = "")
        assertEquals(ApiResult.ServerError(403), blocking { mine() }, "Brak roli to błąd 403")
    }

    @Test
    fun reportsNoNetwork() {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val result = blocking { create(ID, quick) }
        assertInstanceOf(ApiResult.NetworkError::class.java, result, "Brak sieci to NetworkError")
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
