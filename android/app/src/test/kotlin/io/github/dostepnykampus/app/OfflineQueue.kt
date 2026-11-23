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
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.core.network.auth.AccessTokens
import java.io.File
import java.time.Instant
import java.util.Base64
import kotlin.coroutines.EmptyCoroutineContext

/** Kolejka zgłoszeń w [store] z serwerem bez sieci: każda próba wysyłki woła [onSend] i kończy się `Offline`. */
internal fun offlineQueue(store: SubmissionStore, onSend: () -> Unit): SubmissionQueue =
    queueWith(store) { _, _ ->
        onSend()
        ServerReply.Offline
    }

/** Kolejka zgłoszeń w [store] z serwerem, który przyjmuje każde zgłoszenie i potwierdza je jako wysłane. */
internal fun acceptingQueue(store: SubmissionStore): SubmissionQueue = queueWith(store, create = ::sentSubmission)

/** Zgłoszenie [id] z treścią [content] w odpowiedzi serwera, który je przyjął. */
internal fun sentSubmission(id: String, content: SubmissionContent): ServerReply<Submission> =
    ServerReply.Ok(
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.SENT,
            createdAt = Instant.EPOCH,
            content = content,
        ),
    )

/** Kolejka zgłoszeń w [store], która po zapisie woła [onSaved], a na każde zgłoszenie odpowiada [create]. */
internal fun queueWith(
    store: SubmissionStore,
    onSaved: () -> Unit = {},
    create: (String, SubmissionContent) -> ServerReply<Submission>,
): SubmissionQueue =
    SubmissionQueue(
        store = store,
        ioContext = EmptyCoroutineContext,
        server =
            SubmissionServer(
                subject = { TEST_ACCOUNT },
                create = { _, id, content -> create(id, content) },
                putPhoto = { _, _, _, _ -> ServerReply.Ok(Unit) },
                update = { _, _, _, _ -> ServerReply.Missing },
            ),
        onSaved = onSaved,
    )

/** Token konta [TEST_ACCOUNT], tego samego, którym pisze [offlineQueue]. */
internal fun testTokens(): AccessTokens = AccessTokens().apply { signIn(testJwt(TEST_ACCOUNT)) }

private fun testJwt(subject: String): String {
    val payload = Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"$subject"}""".toByteArray())
    return "e30.$payload.e30"
}

internal const val TEST_ACCOUNT = "konto-testowe"

/** Zdjęcie testowe [id] zapisane jako plik w katalogu [dir]. */
internal fun photoDraft(dir: File, id: String): PendingPhotoDraft {
    val file = File(dir, "$id.jpg")
    file.writeBytes(byteArrayOf(1, 2, 3))
    return PendingPhotoDraft(photoId = id, path = file.path)
}
