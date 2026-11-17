/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.data.submission.sync.SubmissionServer
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

/** Kolejka zgłoszeń w [store] z serwerem bez sieci: każda próba wysyłki woła [onSend] i kończy się `Offline`. */
internal fun offlineQueue(store: SubmissionStore, onSend: () -> Unit): SubmissionQueue =
    SubmissionQueue(
        store = store,
        subject = { "konto-testowe" },
        ioContext = EmptyCoroutineContext,
        server =
            SubmissionServer(
                create = { _, _, _ ->
                    onSend()
                    ServerReply.Offline
                },
                putPhoto = { _, _, _, _ -> ServerReply.Ok(Unit) },
                update = { _, _, _, _ -> ServerReply.Missing },
            ),
    )

/** Zdjęcie testowe [id] zapisane jako plik w katalogu [dir]. */
internal fun photoDraft(dir: File, id: String): PendingPhotoDraft {
    val file = File(dir, "$id.jpg")
    file.writeBytes(byteArrayOf(1, 2, 3))
    return PendingPhotoDraft(photoId = id, path = file.path)
}
