/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.BarrierPhoto
import io.github.dostepnykampus.core.designsystem.marking.SeverityLabel
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.designsystem.marking.measuredPlace
import io.github.dostepnykampus.core.designsystem.marking.nearEntrance
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.review.R
import io.github.dostepnykampus.feature.review.queue.kindRes
import io.github.dostepnykampus.feature.review.queue.titleRes

/**
 * Dane zgłoszenia na R2 bez pól do edycji.
 *
 * TODO(#3846): zasłanianie twarzy i tablic na zdjęciu.
 * TODO(#4027): rola i pseudonim autora.
 *
 * Przy zmianie „bariera usunięta” pierwsze zdjęcie stoi obok zdjęcia poprzedniego zgłoszenia.
 *
 * Wymagania: US10, US10.6, BR-14, BR-15, BR-19, NFR-19, NFR-21
 *
 * @param shown zgłoszenie z adresami zdjęć
 */
@Composable
internal fun SubmissionDetails(shown: CheckUiState.Ready) {
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
        val previous = shown.previous
        val photos = if (previous == null) shown.photoUrls else shown.photoUrls.drop(1)
        if (previous != null) RemovedComparison(previous, newPhoto = shown.photoUrls.firstOrNull(), type = type)
        photos.forEach { url ->
            BarrierPhoto(url = url, type = type, modifier = Modifier.fillMaxWidth().padding(top = Odstepy.s))
        }
    }
}
