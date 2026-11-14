/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import java.io.File
import java.time.Instant

/** Zgłoszenia, treści i zdjęcia wspólne dla testów kolejki wysyłki. */
internal object SubmissionFixtures {
    /** Odpowiedź serwera: zgłoszenie [id] przyjęte z treścią [content]. */
    fun server(
        id: String,
        content: SubmissionContent,
    ): Submission =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    /** Jedno zdjęcie [id] zapisane jako plik w katalogu [dir]. */
    fun photo(
        dir: File,
        id: String,
    ): List<PendingPhotoDraft> {
        val file = dir.resolve("$id.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = id, path = file.path))
    }

    /** Dokładne zgłoszenie bariery ze zdjęciem [photoId]. */
    fun precise(photoId: String): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(photoId),
            barrierType = "TB-5",
            severity = 2,
        )
}
