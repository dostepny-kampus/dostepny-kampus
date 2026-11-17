/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.sync

import io.github.dostepnykampus.core.data.server.ServerEdit
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import java.io.File
import java.time.Instant

/** Zgłoszenia, treści i zdjęcia wspólne dla testów kolejki wysyłki. */
internal object SubmissionFixtures {
    /** Odpowiedź serwera: zgłoszenie [id] w wersji [version] z treścią [content]. */
    fun server(id: String, content: SubmissionContent, version: Int = 1): Submission =
        Submission(
            id = id,
            version = version,
            status = SubmissionStatus.SENT,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            content = content,
        )

    /** Serwer, który tworzy zgłoszenie przez [create], a zdjęcia i edycje przyjmuje bez błędu. */
    fun acceptingServer(
        subject: () -> String? = { "konto-testowe" },
        create: suspend (id: String, content: SubmissionContent) -> ServerReply<Submission>,
    ): SubmissionServer =
        submissionServer(
            create = create,
            putPhoto = { _, _, _ -> ServerReply.Ok(Unit) },
            update = { id, _, content -> ServerReply.Ok(ServerEdit.Saved(server(id, content))) },
            subject = subject,
        )

    /** Jedno zdjęcie [id] zapisane jako plik w katalogu [dir]. */
    fun photo(dir: File, id: String): List<PendingPhotoDraft> {
        val file = dir.resolve("$id.jpg")
        file.writeBytes(byteArrayOf(1, 2, 3))
        return listOf(PendingPhotoDraft(photoId = id, path = file.path))
    }

    /** Dokładne zgłoszenie bariery ze zdjęciem [photoId] i stopniem utrudnienia [severity]. */
    fun precise(photoId: String, severity: Int = 2): SubmissionContent =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            photoIds = listOf(photoId),
            barrierType = "TB-5",
            severity = severity,
        )
}

/** Serwer testowy z ustaloną tożsamością, chyba że test dostarczy bieżącą sesję. */
internal fun submissionServer(
    create: suspend (String, SubmissionContent) -> ServerReply<Submission>,
    putPhoto: suspend (String, String, ByteArray) -> ServerReply<Unit>,
    update: suspend (String, Int, SubmissionContent) -> ServerReply<ServerEdit>,
    subject: () -> String? = { "konto-testowe" },
): SubmissionServer =
    SubmissionServer(
        subject = subject,
        create = { _, id, content -> create(id, content) },
        putPhoto = { _, id, photo, jpeg -> putPhoto(id, photo, jpeg) },
        update = { _, id, version, content -> update(id, version, content) },
    )
