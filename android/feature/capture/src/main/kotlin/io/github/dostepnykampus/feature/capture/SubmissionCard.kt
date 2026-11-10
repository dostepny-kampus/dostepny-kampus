/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.data.submissions.SendState
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.nameRes
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT)

/**
 * Pozycja listy C3 (US8.1): status słowem z ikoną na początku, więc TalkBack czyta go przed typem bariery
 * (NFR-18, NFR-21), potem typ, budynek z piętrem, data i powód przy „Nie wysłano” (US9.5).
 *
 * @param item zgłoszenie
 * @param onOpen „Otwórz”: poprawka w C2 (US8.3); `null` bez przycisku
 * @param modifier wymiary nadane przez listę
 */
@Composable
internal fun SubmissionCard(item: OwnSubmissionItem, onOpen: (() -> Unit)?, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
            ) {
                StatusLine(item.state)
                Text(text = typeText(item), style = MaterialTheme.typography.titleMedium)
                Text(placeText(item))
                Text(DAY_FORMAT.format(item.savedAt.atZone(ZoneId.systemDefault())))
                item.refusal?.let { reason -> Text(stringResource(reason.textRes())) }
            }
            if (onOpen != null) {
                OutlinedButton(onClick = onOpen) {
                    Text(stringResource(R.string.moje_otworz))
                    Icon(
                        imageVector = StatusIcons.Open,
                        contentDescription = null,
                        modifier = Modifier.padding(start = Odstepy.s),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusLine(state: SendState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = state.icon(), contentDescription = null)
        Text(
            text = stringResource(state.textRes()),
            modifier = Modifier.padding(start = Odstepy.s),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun typeText(item: OwnSubmissionItem): String =
    stringResource(item.type?.nameRes() ?: R.string.moje_typ_nieznany)

@Composable
@ReadOnlyComposable
private fun placeText(item: OwnSubmissionItem): String {
    val building = item.buildingSymbol ?: return stringResource(R.string.budynek_poza)
    val parts =
        listOfNotNull(
            stringResource(R.string.formularz_budynek, building),
            item.floorNumber?.let { stringResource(R.string.szybkie_pietro, it) },
        )
    return parts.joinToString(", ")
}

private fun SendState.textRes(): Int =
    when (this) {
        SendState.LOCAL -> R.string.moje_w_telefonie
        SendState.SENT -> R.string.moje_wyslane
        SendState.REFUSED -> R.string.moje_nie_wyslano
    }

private fun SendState.icon() =
    when (this) {
        SendState.LOCAL -> StatusIcons.Phone
        SendState.SENT -> StatusIcons.Sent
        SendState.REFUSED -> StatusIcons.Refused
    }

private fun RefusalReason.textRes(): Int =
    when (this) {
        RefusalReason.NO_PERMISSION -> R.string.moje_brak_uprawnien
        RefusalReason.NOT_ACCEPTED -> R.string.moje_nie_przyjal
    }
