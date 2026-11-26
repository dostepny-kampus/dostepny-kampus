/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.conflict

import io.github.dostepnykampus.core.data.submission.own.toOwnSubmissionEntity
import io.github.dostepnykampus.core.data.submission.sync.SubmissionStore
import io.github.dostepnykampus.core.data.submission.sync.deletePhotoFiles
import io.github.dostepnykampus.core.data.submission.sync.serverVersionCopy
import io.github.dostepnykampus.core.data.submission.sync.serverVersionKey
import io.github.dostepnykampus.core.data.submission.sync.toPendingSubmissionEntity
import io.github.dostepnykampus.core.data.submission.sync.toSubmissionContentOrNull
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState.LOCAL
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.fieldsChanged
import kotlin.coroutines.CoroutineContext

/**
 * Konflikty wersji zgłoszeń bieżącego konta i wybór jednej wersji. Dopóki nie ma wyboru, obie wersje czekają
 * w telefonie, a kolejka nie wysyła zgłoszenia.
 *
 * Wymagania: US9.3, BR-10
 */
public interface VersionConflicts {
    /** Obie wersje zgłoszenia [id]; `null`, gdy nie czeka na wybór wersji albo należy do innego konta. */
    public suspend fun find(id: String): VersionConflict?

    /**
     * Zostawia wersję z telefonu: zgłoszenie wraca do wysyłki jako edycja wersji z serwera, a kopia wersji
     * z serwera znika. Bez konfliktu nic nie zmienia.
     */
    public suspend fun keepMine(id: String)

    /**
     * Bierze wersję z serwera: zmiana z telefonu znika razem z plikami zdjęć, a historia dostaje stan z serwera.
     * Bez konfliktu nic nie zmienia.
     */
    public suspend fun takeServer(id: String)
}

/**
 * Konflikty w zgłoszeniach z [store] dla konta [subject]; [onSaved] planuje wysyłkę po wyborze własnej wersji.
 *
 * Wymagania: US9.3, BR-10, NFR-33
 */
internal class StoredVersionConflicts(
    private val store: SubmissionStore,
    private val ioContext: CoroutineContext,
    private val subject: () -> String?,
    private val onSaved: () -> Unit,
) : VersionConflicts {
    override suspend fun find(id: String): VersionConflict? {
        val own = refusedRow(id)?.toSubmissionContentOrNull()
        val server = own?.let { serverVersionCopy(store.conflicts, id, ioContext).restore()?.value }
        return if (own == null || server == null) {
            null
        } else {
            val differences = fieldsChanged(previous = own, current = server.content)
            VersionConflict(id = id, own = own, server = server, differences = differences)
        }
    }

    override suspend fun keepMine(id: String) {
        if (find(id) == null) return
        store.pending.together {
            store.pending.markQueued(id)
            store.history.find(id)?.let { own -> store.history.save(own.copy(state = LOCAL, refusalCode = null)) }
            store.conflicts.delete(serverVersionKey(id))
        }
        runCatching(onSaved)
    }

    override suspend fun takeServer(id: String) {
        val server = find(id)?.server ?: return
        val paths = refusedRow(id)?.run { photos.map { it.path } }.orEmpty()
        store.pending.together {
            store.pending.delete(id)
            store.history.find(id)?.let { own -> store.history.save(serverRow(server, own)) }
            store.conflicts.delete(serverVersionKey(id))
        }
        deletePhotoFiles(paths, ioContext)
    }

    private suspend fun refusedRow(id: String): PendingSubmissionWithPhotos? {
        val owner = subject()
        val row = owner?.let { store.edits.find(id) }
        return row?.takeIf { it.submission.isRefused && it.submission.ownerSub == owner }
    }

    private fun serverRow(server: Submission, own: OwnSubmissionEntity): OwnSubmissionEntity =
        server.content
            .toPendingSubmissionEntity(id = server.id, savedAtMillis = own.savedAtMillis)
            .copy(ownerSub = own.ownerSub)
            .toOwnSubmissionEntity()
            .copy(
                state = OwnSubmissionState.SENT,
                serverVersion = server.version,
                serverStatus = server.status.name,
                rejectionReason = server.rejectionReason,
            )
}
