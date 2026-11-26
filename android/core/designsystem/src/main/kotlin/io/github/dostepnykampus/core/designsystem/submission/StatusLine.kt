/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.submission

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.model.submission.SubmissionStatus

/** Słowo statusu zgłoszenia z serwera, np. „Przyjęte”. Wymagania: NFR-21 */
@StringRes
public fun SubmissionStatus.nameRes(): Int =
    when (this) {
        SubmissionStatus.SENT -> R.string.status_wyslane
        SubmissionStatus.IN_REVIEW -> R.string.status_w_weryfikacji
        SubmissionStatus.ACCEPTED -> R.string.status_przyjete
        SubmissionStatus.REJECTED -> R.string.status_odrzucone
        SubmissionStatus.ARCHIVED -> R.string.status_archiwalne
    }

/** Ikona wzmacniająca słowo statusu zgłoszenia z serwera. Wymagania: NFR-21 */
public fun SubmissionStatus.icon(): ImageVector =
    when (this) {
        SubmissionStatus.SENT -> StatusIcons.Sent
        SubmissionStatus.IN_REVIEW -> StatusIcons.InReview
        SubmissionStatus.ACCEPTED -> StatusIcons.Accepted
        SubmissionStatus.REJECTED -> StatusIcons.Rejected
        SubmissionStatus.ARCHIVED -> StatusIcons.Archived
    }

/**
 * Wiersz statusu: ikona bez opisu dla czytnika i słowo [text] obok niej.
 *
 * Wymagania: NFR-21
 *
 * @param text słowo statusu
 * @param icon ikona wzmacniająca słowo
 * @param modifier położenie wiersza wyznacza wołający
 */
@Composable
public fun StatusLine(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null)
        Text(
            text = text,
            modifier = Modifier.padding(start = Odstepy.s),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
