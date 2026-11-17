/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.pending

import io.github.dostepnykampus.core.data.submission.sync.EditedSubmission
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.database.submission.PendingEditDao
import io.github.dostepnykampus.core.database.submission.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.submission.SubmissionContent

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

/** Zgłoszenia „W telefonie” i „Nie wysłano” do poprawki; wysłanych telefon już nie ma. Wymagania: US8.3, BR-26 */
public fun interface PendingEdits {
    /**
     * Wczytuje zgłoszenie z kolejki.
     *
     * @param id identyfikator zgłoszenia z listy C3
     * @return poprawka albo `null`, gdy zgłoszenia nie ma już w telefonie
     */
    public suspend fun load(id: String): PendingEdit?
}

/** Poprawki z kolejki w bazie Room [store]. */
internal fun pendingEditsOf(store: PendingEditDao, subject: () -> String?): PendingEdits =
    PendingEdits { id ->
        val owner = subject() ?: return@PendingEdits null
        store.find(id)?.takeIf { it.submission.ownerSub == owner && subject() == owner }?.toPendingEdit()
    }

/** Poprawka z wiersza albo `null`, gdy telefon nie odczyta treści; C2 wraca wtedy do listy jak bez zgłoszenia. */
internal fun PendingSubmissionWithPhotos.toPendingEdit(): PendingEdit? =
    toSubmissionContentOrNull()?.let { content ->
        PendingEdit(
            edited = EditedSubmission(id = submission.id, baseVersion = submission.baseVersion),
            content = content,
            photos = photos.map { PendingPhotoDraft(photoId = it.photoId, path = it.path) },
        )
    }
