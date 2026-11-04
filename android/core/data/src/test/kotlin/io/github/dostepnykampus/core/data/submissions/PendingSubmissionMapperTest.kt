/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.PendingPhotoEntity
import io.github.dostepnykampus.core.database.PendingSubmissionEntity
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** Kolejka w bazie Room nie jest kontraktem: złe wartości enuma mają się głośno zepsuć. */
class PendingSubmissionMapperTest {
    @Test
    fun `round-trip daje tę samą treść`() {
        val content = fullContent()

        assertEquals(content, pending(content).toSubmissionContent())
    }

    @Test
    fun `nieznany rodzaj daje błąd`() {
        val saved = withRow { it.copy(kind = "SPEED") }

        assertThrows(IllegalArgumentException::class.java) { saved.toSubmissionContent() }
    }

    @Test
    fun `nieznana grupa daje błąd`() {
        val saved = withRow { it.copy(barrierGroup = "STAIRS") }

        assertThrows(IllegalArgumentException::class.java) { saved.toSubmissionContent() }
    }

    @Test
    fun `nieznane oznaczenie daje błąd`() {
        val saved = withRow { it.copy(entranceMarking = "CLOSED") }

        assertThrows(IllegalArgumentException::class.java) { saved.toSubmissionContent() }
    }

    private fun withRow(row: (PendingSubmissionEntity) -> PendingSubmissionEntity): PendingSubmissionWithPhotos {
        val saved = pending(fullContent())

        return saved.copy(submission = row(saved.submission))
    }

    private fun pending(content: SubmissionContent): PendingSubmissionWithPhotos {
        val id = "0b6f6c1e-8a43-4d5e-9f43-2f1d3c4b5a69"
        val entity = content.toPendingSubmissionEntity(id, savedAtMillis = 7)
        val photos = content.photoIds.map { photo(it, of = id) }
        return PendingSubmissionWithPhotos(entity, photos)
    }

    private fun photo(photoId: String, of: String): PendingPhotoEntity =
        PendingPhotoEntity(
            submissionId = of,
            photoId = photoId,
            path = "zdjecia/$photoId.jpg",
            isUploaded = false,
        )

    private fun fullContent(): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.QUICK,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf("p1", "p2"),
            accuracyMeters = 4.0,
            isManualCorrection = null,
            buildingSymbol = "A-1",
            floor = SubmissionFloor(number = 2, isMezzanine = null),
            barrierGroup = BarrierGroup.OTHER,
            barrierType = null,
            severity = 2,
            description = "Progi przy sekretariacie",
            replacesSubmissionId = null,
            isBarrierRemoved = null,
            entranceId = null,
            entranceMarking = EntranceMarking.ACCESSIBLE,
        )
}
