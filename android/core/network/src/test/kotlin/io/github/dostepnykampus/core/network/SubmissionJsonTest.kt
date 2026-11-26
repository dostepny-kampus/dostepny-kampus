/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.SubmissionAuthor
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.submission.decodeSubmission
import io.github.dostepnykampus.core.network.submission.encodeNewSubmission
import io.github.dostepnykampus.core.network.submission.encodeSubmissionUpdate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/**
 * JSON zgłoszeń według kontraktu: `NewSubmission`, `SubmissionUpdate` i `Submission`, bez sieci.
 *
 * Wymagania: BR-9, BR-10, BR-5, US7, US8.1
 */
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
    fun writesPhoneIdForNew() {
        val sent = json.parseToJsonElement(json.encodeNewSubmission(id = ID, content = quick)).jsonObject
        assertEquals("\"$ID\"", sent.getValue("id").toString(), "identyfikator telefonu w nowym zgłoszeniu")
        val location = sent.getValue("location").jsonObject
        assertEquals(
            "[17.06209,51.10785]",
            location.getValue("coordinates").toString(),
            "współrzędne w nowym zgłoszeniu",
        )
    }

    @Test
    fun omitsEmptyFieldsForNew() {
        val sent = json.parseToJsonElement(json.encodeNewSubmission(id = ID, content = quick)).jsonObject
        assertFalse("severity" in sent, "brak ważności w wysyłce")
        assertFalse("buildingSymbol" in sent, "brak budynku w wysyłce")
    }

    @Test
    fun writesBaseVersionForUpdate() {
        val sent = json.parseToJsonElement(json.encodeSubmissionUpdate(version = 2, content = quick)).jsonObject
        assertEquals("2", sent.getValue("version").toString(), "wersja bazowa aktualizacji")
    }

    @Test
    fun roundTripsFullSubmission() {
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
        assertEquals(precise, back.content, "treść po pełnym obiegu")
        assertEquals(Instant.parse(TIME), back.verifiedAt, "data weryfikacji po obiegu")
    }

    @Test
    fun readsServerSubmission() {
        val submission = json.decodeSubmission(SERVER)
        assertEquals(SubmissionStatus.SENT, submission.status, "status zgłoszenia z serwera")
        assertEquals(Instant.parse("2026-11-03T18:00:00Z"), submission.createdAt, "data utworzenia z serwera")
        assertEquals(quick.copy(buildingSymbol = "A-1"), submission.content, "treść zgłoszenia z serwera")
    }

    @Test
    fun readsAuthorForVerifier() {
        val author = """"author":{"pseudonym":"Autor 14","roles":["VISITOR","COLLECTOR"]},"photoIds""""
        val withAuthor = decode(SERVER.replace(oldValue = "\"photoIds\"", newValue = author))
        val expected = SubmissionAuthor(pseudonym = "Autor 14", staffRoles = setOf(StaffRole.COLLECTOR))
        assertEquals(expected, withAuthor.author, "pseudonim i role pracownicze autora")
        assertNull(decode(SERVER).author, "zgłoszenie bez pola author")
    }

    @Test
    fun rejectsBadServerSubmission() {
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
