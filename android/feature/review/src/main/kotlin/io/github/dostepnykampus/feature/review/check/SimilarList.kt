/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StatusIcons
import io.github.dostepnykampus.core.designsystem.marking.BarrierPhoto
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import io.github.dostepnykampus.feature.review.R
import io.github.dostepnykampus.feature.review.queue.titleRes
import kotlin.math.roundToInt
import io.github.dostepnykampus.core.designsystem.R as DesignR

/**
 * „Podobne zgłoszenia do 10 m”: typ, zdjęcie, odległość tekstem, piętro i status tekstem z ikoną.
 *
 * Wymagania: US10.1, BR-13, NFR-19, NFR-21
 *
 * @param similar podobne zgłoszenia od najbliższego
 * @param photos adres pierwszego zdjęcia podobnego zgłoszenia według jego identyfikatora
 */
@Composable
internal fun SimilarList(similar: List<SimilarSubmission>, photos: Map<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.sprawdzenie_podobne),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        if (similar.isEmpty()) GroupFrame { Text(stringResource(R.string.sprawdzenie_brak_podobnych)) }
        similar.forEach { near -> SimilarCard(near = near, photo = photos[near.submission.id]) }
    }
}

@Composable
private fun SimilarCard(near: SimilarSubmission, photo: String?) {
    val content = near.submission.content
    GroupFrame {
        BarrierPhoto(
            url = photo,
            type = content.barrierType?.let { BarrierType.fromCode(it) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(content.titleRes()), style = MaterialTheme.typography.titleMedium)
        Text(distanceAndFloor(near))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = near.submission.status.icon(), contentDescription = null)
            Text(stringResource(near.submission.status.textRes()))
        }
    }
}

@Composable
@ReadOnlyComposable
private fun distanceAndFloor(near: SimilarSubmission): String {
    val distance = stringResource(R.string.sprawdzenie_odleglosc, near.distanceMeters.roundToInt())
    val number = near.submission.content.floor?.number ?: return distance
    val floor =
        if (number == 0) {
            stringResource(DesignR.string.miejsce_parter)
        } else {
            stringResource(DesignR.string.miejsce_pietro, number)
        }
    return "$distance, $floor"
}

private fun SubmissionStatus.textRes(): Int =
    when (this) {
        SubmissionStatus.SENT -> R.string.status_wyslane
        SubmissionStatus.IN_REVIEW -> R.string.status_w_weryfikacji
        SubmissionStatus.ACCEPTED -> R.string.status_przyjete
        SubmissionStatus.REJECTED -> R.string.status_odrzucone
        SubmissionStatus.ARCHIVED -> R.string.status_archiwalne
    }

private fun SubmissionStatus.icon(): ImageVector =
    when (this) {
        SubmissionStatus.SENT -> StatusIcons.Sent
        SubmissionStatus.IN_REVIEW -> StatusIcons.InReview
        SubmissionStatus.ACCEPTED -> StatusIcons.Accepted
        SubmissionStatus.REJECTED -> StatusIcons.Rejected
        SubmissionStatus.ARCHIVED -> StatusIcons.Archived
    }
