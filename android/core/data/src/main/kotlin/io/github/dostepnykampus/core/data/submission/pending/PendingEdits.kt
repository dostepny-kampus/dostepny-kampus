/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.pending

import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.ownCopyOf
import io.github.dostepnykampus.core.data.submission.sync.toSubmissionContentOrNull
import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import java.io.File

/**
 * Własne zgłoszenie z kolejki telefonu otwarte do poprawki w C2.
 *
 * Wymagania: US8.3
 *
 * @property edited identyfikator i wersja bazowa; zapis przez `SubmissionQueue.saveEdit` zastępuje to zgłoszenie
 * @property content zapisana treść
 * @property photos zdjęcia w kolejności [SubmissionContent.photoIds]
 */
public data class PendingEdit(
    val edited: EditedSubmission,
    val content: SubmissionContent,
    val photos: List<PendingPhotoDraft>,
)

/**
 * Zgłoszenia „W telefonie”, „Nie wysłano” i wysłane przed decyzją do poprawki; wysłane bez treści w historii
 * (sprzed jej zapisu) telefon już nie ma.
 *
 * Wymagania: US8.3, BR-26
 */
public fun interface PendingEdits {
    /**
     * Wczytuje zgłoszenie z kolejki.
     *
     * @param id identyfikator zgłoszenia z listy C3
     * @return poprawka albo `null`, gdy zgłoszenia nie ma już w telefonie
     */
    public suspend fun load(id: String): PendingEdit?
}

/**
 * Poprawki z kolejki w bazie Room [store], a zgłoszenia już wysłane przed decyzją z historii [history].
 *
 * Wymagania: US8.3, BR-10, NFR-5
 */
internal fun pendingEditsOf(
    store: PendingEditDao,
    history: OwnSubmissionDao? = null,
    subject: () -> String?,
): PendingEdits =
    PendingEdits { id ->
        subject()?.let { owner ->
            val queued = store.find(id)?.takeIf { it.submission.ownerSub == owner }
            val edit = if (queued != null) queued.toPendingEdit() else history?.find(id)?.toSentEdit(owner)
            edit?.takeIf { subject() == owner }
        }
    }

/**
 * Poprawka wysłanego zgłoszenia konta [owner] przed decyzją z wersją bazową z serwera; zdjęcie, którego pliku już
 * nie ma, wypada. `null` dla innego konta, po decyzji i bez treści w historii.
 */
private fun OwnSubmissionEntity.toSentEdit(owner: String): PendingEdit? {
    val copy = ownCopyOf(contentJson)
    val isOpen = ownerSub == owner && serverStatus !in DECIDED
    if (copy == null || !isOpen) return null
    val photos =
        copy.content.photoIds
            .zip(copy.photoPaths) { photoId, path -> PendingPhotoDraft(photoId = photoId, path = path) }
            .filter { File(it.path).exists() }
    return PendingEdit(
        edited = EditedSubmission(id = id, baseVersion = serverVersion),
        content = copy.content.copy(photoIds = photos.map { it.photoId }),
        photos = photos,
    )
}

private val DECIDED = setOf(SubmissionStatus.ACCEPTED.name, SubmissionStatus.ARCHIVED.name)

/** Poprawka z wiersza albo `null`, gdy telefon nie odczyta treści; C2 wraca wtedy do listy jak bez zgłoszenia. */
internal fun PendingSubmissionWithPhotos.toPendingEdit(): PendingEdit? =
    toSubmissionContentOrNull()?.let { content ->
        PendingEdit(
            edited = EditedSubmission(id = submission.id, baseVersion = submission.baseVersion),
            content = content,
            photos = photos.map { PendingPhotoDraft(photoId = it.photoId, path = it.path) },
        )
    }
