/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia

/**
 * Krok 3 V4 przed „Wyślij”: miniatura zdjęcia, wybrana grupa, piętro albo „Nie wiem”, jeśli było pytanie,
 * i zdanie o dołączeniu bieżącego miejsca. Gdy pozycja jest jeszcze nieznana,
 * zamiast zdania „Ustalamy, gdzie jesteś…”; „Wyślij” wtedy czeka. Przy „Brak pozycji” komunikat mówi,
 * co zrobić; zdjęcie i grupa zostają.
 *
 * Wymagania: US4.1, US4.5, NFR-27, NFR-23, UC7
 *
 * @param report zgłoszenie po kroku 2
 * @param isFloorAsked czy krok 2 pytał o piętro
 * @param lookup ustalanie bieżącej pozycji
 */
@Composable
internal fun QuickSendSummary(report: QuickReport, isFloorAsked: Boolean, lookup: QuickLookup) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        PhotoPreviews(PhotoPaths(listOf(report.photo.path)))
        Text(stringResource(report.group.quickLabel()))
        if (isFloorAsked) Text(report.floorLabel ?: floorLabel(report.floorNumber))
        when (lookup) {
            is QuickLookup.Found -> {
                Text(stringResource(R.string.szybkie_dolaczymy_miejsce))
            }

            QuickLookup.Locating -> {
                Text(stringResource(R.string.szybkie_ustalamy_miejsce))
            }

            QuickLookup.Lost -> {
                Text(
                    text = stringResource(R.string.szybkie_brak_pozycji),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = Typografia.wyroznienieWaga,
                )
            }
        }
    }
}

/**
 * Potwierdzenie po „Wyślij”: zgłoszenie trafi do weryfikacji, dalszego statusu Odwiedzający nie widzi.
 * Bez internetu: zdjęcie czeka w telefonie i wyśle się samo po powrocie sieci.
 *
 * Wymagania: US4.4, US4.2, NFR-5
 *
 * @param isSavedOffline zapis w kolejce bez internetu
 */
@Composable
internal fun QuickSentMessage(isSavedOffline: Boolean = false) {
    Text(stringResource(if (isSavedOffline) R.string.szybkie_zapisane_bez_sieci else R.string.szybkie_dziekujemy))
}

@Composable
@ReadOnlyComposable
private fun floorLabel(floor: Int?): String =
    when (floor) {
        null -> stringResource(R.string.szybkie_nie_wiem)
        0 -> stringResource(R.string.szybkie_parter)
        else -> stringResource(R.string.szybkie_pietro, floor)
    }
