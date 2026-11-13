/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

/** Zgłoszenie z serwera jako wartość: reguły treści i wersji z kontraktu. Wymagania: BR-7, BR-8, BR-10, BR-24 */
class SubmissionTest {
    private val quick =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"),
            barrierGroup = BarrierGroup.OTHER,
        )

    @Test
    fun br8_quick_without_photo() {
        assertThrows<IllegalArgumentException> { quick.copy(photoIds = emptyList()) }
    }

    @Test
    fun br7_severity_above_three() {
        assertThrows<IllegalArgumentException> { quick.copy(severity = 4) }
    }

    @Test
    fun br24_marking_never_no_data() {
        assertThrows<IllegalArgumentException> { quick.copy(entranceMarking = EntranceMarking.NO_DATA) }
    }

    @Test
    fun br10_version_starts_at_one() {
        assertThrows<IllegalArgumentException> { submission(version = 0) }
    }

    @Test
    fun br10_edit_is_next_version() {
        val first = submission(version = 1)
        val edited = first.copy(version = 2, content = quick.copy(severity = 3))
        assertEquals(first.id, edited.id)
        assertEquals(2, edited.version)
    }

    private fun submission(version: Int): Submission =
        Submission(
            id = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69",
            version = version,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-11-03T18:00:00Z"),
            content = quick,
        )
}
