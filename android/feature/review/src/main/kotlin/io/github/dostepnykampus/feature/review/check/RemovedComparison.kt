/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.BarrierPhoto
import io.github.dostepnykampus.core.designsystem.submission.StatusLine
import io.github.dostepnykampus.core.designsystem.submission.icon
import io.github.dostepnykampus.core.designsystem.submission.nameRes
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.feature.review.R

/**
 * Zmiana „bariera usunięta”: zdjęcie poprzedniego zgłoszenia obok nowego z podpisami, przy dużej czcionce jedno pod
 * drugim, i status poprzedniego zgłoszenia tekstem i ikoną.
 *
 * Wymagania: US10.6, US10.8, BR-19, BR-23, NFR-20, NFR-21
 *
 * @param previous poprzednie zgłoszenie
 * @param newPhoto adres nowego zdjęcia miejsca; `null` bez zdjęcia
 * @param type typ bariery do opisu zdjęć
 */
@Composable
internal fun RemovedComparison(previous: PreviousSubmission, newPhoto: String?, type: BarrierType?) {
    val isStacked = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        if (isStacked) {
            SignedPhoto(url = previous.photoUrl, caption = R.string.sprawdzenie_zdjecie_poprzednie, type = type)
            SignedPhoto(url = newPhoto, caption = R.string.sprawdzenie_zdjecie_nowe, type = type)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s)) {
                SignedPhoto(
                    url = previous.photoUrl,
                    caption = R.string.sprawdzenie_zdjecie_poprzednie,
                    type = type,
                    modifier = Modifier.weight(1f),
                )
                SignedPhoto(
                    url = newPhoto,
                    caption = R.string.sprawdzenie_zdjecie_nowe,
                    type = type,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(stringResource(R.string.sprawdzenie_poprzednie), style = MaterialTheme.typography.bodyLarge)
        StatusLine(text = stringResource(previous.status.nameRes()), icon = previous.status.icon())
    }
}

@Composable
private fun SignedPhoto(
    url: String?,
    @StringRes caption: Int,
    type: BarrierType?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        BarrierPhoto(url = url, type = type, modifier = Modifier.fillMaxWidth())
        Text(stringResource(caption), style = MaterialTheme.typography.bodyMedium)
    }
}
