/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StatusLine
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.card.ListItemCard
import io.github.dostepnykampus.core.designsystem.icon
import io.github.dostepnykampus.core.designsystem.marking.BarrierPhoto
import io.github.dostepnykampus.core.designsystem.marking.floorText
import io.github.dostepnykampus.core.designsystem.nameRes
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.feature.review.R
import io.github.dostepnykampus.feature.review.queue.titleRes
import kotlin.math.roundToInt

/**
 * Podobne zgłoszenia z odległością tekstem, a bez nich komunikat o ich braku.
 *
 * Wymagania: US10.1, BR-13, NFR-19, NFR-21
 *
 * @param similar podobne zgłoszenia ze zdjęciami
 */
@Composable
internal fun SimilarList(similar: SimilarReports) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.sprawdzenie_podobne),
            modifier = Modifier.padding(top = Odstepy.m).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        if (similar.items.isEmpty()) {
            Text(stringResource(R.string.sprawdzenie_brak_podobnych), style = MaterialTheme.typography.bodyLarge)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
            similar.items.forEach { near -> SimilarCard(near = near, photo = similar.photos[near.submission.id]) }
        }
    }
}

@Composable
private fun SimilarCard(near: SimilarSubmission, photo: String?) {
    val content = near.submission.content
    val status = near.submission.status
    ListItemCard {
        BarrierPhoto(
            url = photo,
            type = content.barrierType?.let { BarrierType.fromCode(it) },
            modifier = Modifier.fillMaxWidth().padding(vertical = Odstepy.s),
        )
        Text(
            text = stringResource(content.titleRes()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        Text(distanceAndFloor(near), style = MaterialTheme.typography.bodyLarge)
        StatusLine(
            text = stringResource(status.nameRes()),
            icon = status.icon(),
            modifier = Modifier.padding(vertical = Odstepy.s),
        )
    }
}

@Composable
@ReadOnlyComposable
private fun distanceAndFloor(near: SimilarSubmission): String {
    val distance = stringResource(R.string.sprawdzenie_odleglosc, near.distanceMeters.roundToInt())
    val floor = near.submission.content.floor?.let { floorText(it) }
    return if (floor == null) distance else "$distance, $floor"
}
