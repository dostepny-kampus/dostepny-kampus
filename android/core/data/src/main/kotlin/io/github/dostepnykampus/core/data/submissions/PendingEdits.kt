/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.PendingEditDao
import io.github.dostepnykampus.core.database.PendingSubmissionWithPhotos
import io.github.dostepnykampus.core.model.SubmissionContent

/**
 * Własne zgłoszenie z kolejki telefonu otwarte do poprawki w C2 (US8.3).
 *
 * @property edited identyfikator i wersja bazowa; zapis przez `SubmissionQueue.saveEdit` zastępuje to zgłoszenie
 * @property content zapisana treść
 * @property photos zdjęcia w kolejności [SubmissionContent.photoIds]
 */
data class PendingEdit(
    val edited: EditedSubmission,
    val content: SubmissionContent,
    val photos: List<PendingPhotoDraft>,
)

/** Zgłoszenia „W telefonie” i „Nie wysłano” do poprawki; wysłanych telefon już nie ma (US8.3, BR-26). */
fun interface PendingEdits {
    /**
     * Wczytuje zgłoszenie z kolejki.
     *
     * @param id identyfikator zgłoszenia z listy C3
     * @return poprawka albo `null`, gdy zgłoszenia nie ma już w telefonie
     */
    suspend fun load(id: String): PendingEdit?
}

/** Poprawki z kolejki w bazie Room [store]. */
fun pendingEditsOf(store: PendingEditDao): PendingEdits = PendingEdits { id -> store.find(id)?.toPendingEdit() }

internal fun PendingSubmissionWithPhotos.toPendingEdit(): PendingEdit =
    PendingEdit(
        edited = EditedSubmission(id = submission.id, baseVersion = submission.baseVersion),
        content = toSubmissionContent(),
        photos = photos.map { PendingPhotoDraft(photoId = it.photoId, path = it.path) },
    )
