/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.dostepnykampus.core.data.submission.own.SendState
import io.github.dostepnykampus.core.designsystem.StatusIcons
import io.github.dostepnykampus.feature.capture.R

/**
 * Status na karcie C3: słowo i ikona. Stan z telefonu ma pierwszeństwo, bo poprawka czeka w kolejce;
 * po wysłaniu karta pokazuje status z serwera, gdy telefon już go zna.
 *
 * Wymagania: NFR-21, US8.1, US8.2
 *
 * @property textRes słowo statusu
 * @property icon ikona wzmacniająca słowo
 */
internal enum class ShownStatus(
    @param:StringRes val textRes: Int,
    val icon: ImageVector,
) {
    /** „W telefonie”. */
    LOCAL(R.string.moje_w_telefonie, StatusIcons.Phone),

    /** „Nie wysłano”. */
    REFUSED(R.string.moje_nie_wyslano, StatusIcons.Refused),

    /** „Wysłane”. */
    SENT(R.string.moje_wyslane, StatusIcons.Sent),

    /** „W weryfikacji”. */
    IN_REVIEW(R.string.moje_w_weryfikacji, StatusIcons.InReview),

    /** „Przyjęte”. */
    ACCEPTED(R.string.moje_przyjete, StatusIcons.Accepted),

    /** „Odrzucone” z powodem. Wymagania: BR-16 */
    REJECTED(R.string.moje_odrzucone, StatusIcons.Rejected),

    /** „Archiwalne”. */
    ARCHIVED(R.string.moje_archiwalne, StatusIcons.Archived),
}

internal fun OwnSubmissionItem.shownStatus(): ShownStatus =
    when (state) {
        SendState.LOCAL -> ShownStatus.LOCAL
        SendState.REFUSED -> ShownStatus.REFUSED
        SendState.SENT -> serverStatus?.let { ShownStatus.valueOf(it.name) } ?: ShownStatus.SENT
    }

/**
 * Czy zgłoszenie można otworzyć do poprawki w C2: z telefonu oraz wysłane przed przyjęciem albo archiwizacją,
 * zgodnie z regułą edycji na serwerze.
 *
 * Wymagania: US8.1, US8.3
 *
 * @param status status widoczny na karcie
 */
internal fun canOpen(status: ShownStatus): Boolean =
    when (status) {
        ShownStatus.LOCAL,
        ShownStatus.REFUSED,
        ShownStatus.SENT,
        ShownStatus.IN_REVIEW,
        ShownStatus.REJECTED,
        -> true

        ShownStatus.ACCEPTED,
        ShownStatus.ARCHIVED,
        -> false
    }
