/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.core.designsystem.marking.BarrierPhoto
import io.github.dostepnykampus.core.designsystem.marking.SeverityLabel
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.designsystem.marking.measuredPlace
import io.github.dostepnykampus.core.designsystem.marking.nearEntrance
import io.github.dostepnykampus.core.designsystem.nameRes
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.review.R
import io.github.dostepnykampus.feature.review.queue.kindRes
import io.github.dostepnykampus.feature.review.queue.titleRes

/**
 * Dane zgłoszenia na R2 bez pól do edycji.
 *
 * Przy zmianie „bariera usunięta” pierwsze zdjęcie stoi obok zdjęcia poprzedniego zgłoszenia.
 *
 * Wymagania: US10, US10.6, US17.2, BR-5, BR-14, BR-15, BR-19, BR-24, NFR-19, NFR-21
 *
 * @param shown zgłoszenie z adresami zdjęć
 * @param onCover „Zasłoń twarz lub tablicę” przy zdjęciu o identyfikatorze i adresie
 */
@Composable
internal fun SubmissionDetails(shown: CheckUiState.Ready, onCover: (photoId: String, photoUrl: String) -> Unit) {
    val content = shown.submission.content
    val type = content.barrierType?.let { BarrierType.fromCode(it) }
    GroupFrame {
        Text(stringResource(content.kindRes()), style = MaterialTheme.typography.titleSmall)
        if (content.kind == SubmissionKind.QUICK) Text(stringResource(R.string.sprawdzenie_czego_dotyczy))
        Text(
            text = stringResource(content.titleRes()),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
        )
        content.severity?.let { Severity.fromValue(it) }?.let { severity ->
            SeverityLabel(severity = severity, modifier = Modifier.padding(vertical = Odstepy.s))
        }
        Text(
            text = barrierPlace(symbol = content.buildingSymbol, name = null, entrance = null, floor = content.floor),
            style = MaterialTheme.typography.bodyLarge,
        )
        val accuracy = content.accuracyMeters
        if (accuracy != null) {
            val near = shown.nearest?.let { nearEntrance(entrance = it.entrance, meters = it.meters) }
            Text(stringResource(R.string.sprawdzenie_miejsce, measuredPlace(near = near, accuracyMeters = accuracy)))
        }
        if (content.kind == SubmissionKind.ENTRANCE_MARKING) MarkingChange(shown.current, content.entranceMarking)
        SubmissionPhotos(shown = shown, type = type, onCover = onCover)
        shown.submission.author?.let { author ->
            val roles = StaffRole.entries.filter { it in author.staffRoles }.map { stringResource(it.nameRes()) }
            val who = (roles + author.pseudonym).joinToString(separator = ", ")
            Text(stringResource(R.string.sprawdzenie_autor, who), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SubmissionPhotos(
    shown: CheckUiState.Ready,
    type: BarrierType?,
    onCover: (photoId: String, photoUrl: String) -> Unit,
) {
    val ids = shown.submission.content.photoIds
    val photos = ids.zip(shown.photoUrls)
    val previous = shown.previous
    val compared = previous?.let { photos.firstOrNull() }
    if (previous != null) {
        RemovedComparison(previous = previous, newPhoto = compared?.second, type = type) {
            if (compared != null) CoverPhotoAction { onCover(compared.first, compared.second) }
        }
    }
    val remaining = if (compared == null) photos else photos.drop(1)
    remaining.forEach { (photoId, url) ->
        BarrierPhoto(url = url, type = type, modifier = Modifier.fillMaxWidth().padding(top = Odstepy.s))
        CoverPhotoAction { onCover(photoId, url) }
    }
}

@Composable
private fun CoverPhotoAction(onClick: () -> Unit) {
    OutlinedAction(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = Odstepy.s).heightIn(min = CeleDotyku.przycisk),
    ) {
        Text(stringResource(R.string.zaslona_naglowek))
    }
}
