/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/** JSON zgłoszeń według kontraktu: `NewSubmission`, `SubmissionUpdate` i `Submission`, bez sieci. */
class SubmissionJsonTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val quick =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(PHOTO),
            barrierGroup = BarrierGroup.OTHER,
        )

    @Test
    fun br9_new_has_phone_id() {
        val sent = json.parseToJsonElement(json.encodeNewSubmission(id = ID, content = quick)).jsonObject
        assertEquals("\"$ID\"", sent.getValue("id").toString())
        val location = sent.getValue("location").jsonObject
        assertEquals("[17.06209,51.10785]", location.getValue("coordinates").toString())
    }

    @Test
    fun br9_new_omits_empty_fields() {
        val sent = json.parseToJsonElement(json.encodeNewSubmission(id = ID, content = quick)).jsonObject
        assertFalse("severity" in sent)
        assertFalse("buildingSymbol" in sent)
    }

    @Test
    fun br10_update_has_base_version() {
        val sent = json.parseToJsonElement(json.encodeSubmissionUpdate(version = 2, content = quick)).jsonObject
        assertEquals("2", sent.getValue("version").toString())
    }

    @Test
    fun us7_full_round_trip() {
        val precise =
            quick.copy(
                kind = SubmissionKind.PRECISE,
                accuracyMeters = 2.5,
                isManualCorrection = true,
                buildingSymbol = "C-13",
                floor = SubmissionFloor(floorId = "C-13-1", number = 1, isMezzanine = true),
                barrierType = "TB-29",
                severity = 3,
                description = "Brak poręczy",
                replacesSubmissionId = ID,
                isBarrierRemoved = true,
                entranceId = "node/1",
                entranceMarking = EntranceMarking.ACCESSIBLE,
            )
        val sent = json.encodeNewSubmission(id = ID, content = precise)
        val meta = """"version":1,"status":"ACCEPTED","createdAt":"$TIME","verifiedAt":"$TIME","""
        val back = decode(sent.replaceFirst(oldValue = "{", newValue = "{$meta"))
        assertEquals(precise, back.content)
        assertEquals(Instant.parse(TIME), back.verifiedAt)
    }

    @Test
    fun us8_1_server_submission() {
        val submission = json.decodeSubmission(SERVER)
        assertEquals(SubmissionStatus.SENT, submission.status)
        assertEquals(Instant.parse("2026-11-03T18:00:00Z"), submission.createdAt)
        assertEquals(quick.copy(buildingSymbol = "A-1"), submission.content)
    }

    @Test
    fun us8_1_bad_server_submission() {
        assertThrows<IllegalArgumentException> { decode(SERVER.replace(oldValue = "SENT", newValue = "LOST")) }
        assertThrows<IllegalArgumentException> { decode(SERVER.replace(oldValue = "18:00:00Z", newValue = "wczoraj")) }
        assertThrows<IllegalArgumentException> { decode(SERVER.replace(oldValue = "\"$PHOTO\"", newValue = "")) }
    }

    private fun decode(body: String) = json.decodeSubmission(body)

    private companion object {
        const val ID = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
        const val TIME = "2026-11-03T18:00:00Z"
        const val PHOTO = "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
        const val SERVER =
            """{"id":"$ID","version":1,"status":"SENT","createdAt":"2026-11-03T18:00:00Z","kind":"QUICK",""" +
                """"location":{"type":"Point","coordinates":[17.06209,51.10785]},"barrierGroup":"OTHER",""" +
                """"buildingSymbol":"A-1","photoIds":["$PHOTO"]}"""
    }
}
