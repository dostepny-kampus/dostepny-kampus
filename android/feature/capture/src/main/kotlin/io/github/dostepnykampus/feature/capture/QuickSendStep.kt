/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.Odstepy

/**
 * Krok 3 V4 przed „Wyślij”: miniatura zdjęcia, wybrana grupa, piętro albo „Nie wiem”, jeśli było pytanie,
 * i zdanie o dołączeniu bieżącego miejsca (US4.1, US4.5, NFR-27). Gdy pozycja jest jeszcze nieznana,
 * zamiast zdania „Ustalamy, gdzie jesteś…”; „Wyślij” wtedy czeka (NFR-23).
 *
 * @param report zgłoszenie po kroku 2
 * @param isFloorAsked czy krok 2 pytał o piętro
 * @param isPositionKnown czy telefon podał już bieżącą pozycję
 */
@Composable
internal fun QuickSendSummary(report: QuickReport, isFloorAsked: Boolean, isPositionKnown: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        PhotoPreviews(PhotoPaths(listOf(report.photo.path)))
        Text(stringResource(report.group.quickLabel()))
        if (isFloorAsked) Text(floorLabel(report.floorNumber))
        val place = if (isPositionKnown) R.string.szybkie_dolaczymy_miejsce else R.string.szybkie_ustalamy_miejsce
        Text(stringResource(place))
    }
}

/** Potwierdzenie po „Wyślij”: zgłoszenie trafi do weryfikacji, dalszego statusu Odwiedzający nie widzi (US4.4). */
@Composable
internal fun QuickSentMessage() {
    Text(stringResource(R.string.szybkie_dziekujemy))
}

@Composable
private fun floorLabel(floor: Int?): String =
    when (floor) {
        null -> stringResource(R.string.szybkie_nie_wiem)
        0 -> stringResource(R.string.szybkie_parter)
        else -> stringResource(R.string.szybkie_pietro, floor)
    }
