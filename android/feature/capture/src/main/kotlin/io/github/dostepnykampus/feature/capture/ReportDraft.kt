/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.core.data.submissions.EditedSubmission
import io.github.dostepnykampus.core.data.submissions.PendingPhotoDraft
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.Severity
import kotlinx.serialization.Serializable

/** Pola C2 wspólne dla formularza i powrotu przez C1 (US7.4). Stan zapisuje właściciel nawigacji. */
@Serializable
class ReportDraft {
    private var saved: DraftFields? = null

    internal fun keep(state: ReportUiState) {
        saved = DraftFields(state)
    }

    internal fun restore(form: ReportForm): ReportUiState = saved?.toUiState(form) ?: ReportUiState(form = form)

    internal fun clear() {
        saved = null
    }
}

@Serializable
private data class DraftPhoto(val id: String, val path: String)

@Serializable
private data class DraftEdit(val id: String, val baseVersion: Int?)

@Serializable
private data class DraftFields(
    val buildingSymbol: String?,
    val floor: ReportFloor?,
    val barrierType: BarrierType?,
    val severity: Severity?,
    val photoIds: List<String>,
    val photos: List<DraftPhoto>,
    val description: String,
    val replacesSubmissionId: String?,
    val edited: DraftEdit?,
    val isSaveAttempted: Boolean,
) {
    constructor(state: ReportUiState) : this(
        buildingSymbol = state.form.place.buildingSymbol,
        floor = state.form.floor,
        barrierType = state.form.barrierType,
        severity = state.form.severity,
        photoIds = state.form.photoIds,
        photos = state.photos.map { DraftPhoto(it.photoId, it.path) },
        description = state.form.description,
        replacesSubmissionId = state.form.replacesSubmissionId,
        edited = state.form.editedSubmission?.let { DraftEdit(it.id, it.baseVersion) },
        isSaveAttempted = state.isSaveAttempted,
    )

    fun toUiState(form: ReportForm): ReportUiState {
        val restored =
            form.copy(
                floor = floor.takeIf { buildingSymbol == form.place.buildingSymbol },
                barrierType = barrierType,
                severity = severity,
                photoIds = photoIds,
                description = description,
                replacesSubmissionId = replacesSubmissionId,
                editedSubmission = edited?.let { EditedSubmission(it.id, it.baseVersion) },
            )
        return ReportUiState(
            form = restored,
            photos = photos.map { PendingPhotoDraft(photoId = it.id, path = it.path) },
            errors = if (isSaveAttempted) restored.errors() else emptyList(),
            isSaveAttempted = isSaveAttempted,
        )
    }
}
