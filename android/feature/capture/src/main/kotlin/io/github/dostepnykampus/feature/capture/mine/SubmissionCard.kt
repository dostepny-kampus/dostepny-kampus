/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

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
import io.github.dostepnykampus.core.data.submission.own.RefusalReason
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StatusIcons
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.R
import java.time.ZoneId

/**
 * Pozycja listy C3: status słowem z ikoną na początku, więc TalkBack czyta go przed typem bariery,
 * potem rodzaj albo typ, budynek z piętrem, data i powód przy „Nie wysłano”. Odrzucone jako „nieaktualne” ma
 * zdanie o nowszej wersji, a zgłoszenie z pomiaru zamiast „Otwórz” ma „Wyślij ponownie”.
 * TODO(#3849): „Bariery już nie ma”, „Nowe oznaczenie:” i „Wyślij ponownie” zmiany oznaczenia do C6.
 *
 * Wymagania: US8.1, NFR-18, NFR-21, US9.5, US8.3, US17.2, BR-23
 *
 * @param item zgłoszenie
 * @param onOpen „Otwórz” albo „Wyślij ponownie”: poprawka w C2 dla zgłoszeń, które można poprawić; `null` bez
 *   przycisku
 * @param modifier wymiary nadane przez listę
 */
@Composable
internal fun SubmissionCard(item: OwnSubmissionItem, onOpen: (() -> Unit)?, modifier: Modifier = Modifier) {
    val status = item.shownStatus()
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            SubmissionDetails(item = item, status = status)
            if (onOpen != null && canOpen(status)) OpenButton(isOutdated = item.isOutdated, onClick = onOpen)
        }
    }
}

@Composable
private fun SubmissionDetails(item: OwnSubmissionItem, status: ShownStatus) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
    ) {
        StatusLine(status)
        if (item.isChangeWaiting) Text(stringResource(R.string.moje_zmiana_czeka))
        Text(text = titleText(item), style = MaterialTheme.typography.titleMedium)
        Text(placeText(item))
        Text(campusDay(item.savedAt.atZone(ZoneId.systemDefault()).toLocalDate()))
        item.refusal?.let { reason -> Text(stringResource(reason.textRes())) }
        item.rejectionReason?.takeIf { status == ShownStatus.REJECTED }?.let { reason ->
            Text(stringResource(R.string.moje_powod_odrzucenia, reason))
        }
        if (item.isOutdated) Text(stringResource(outdatedText(item.kind)))
    }
}

@Composable
private fun OpenButton(isOutdated: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick) {
        Text(stringResource(if (isOutdated) R.string.moje_wyslij_ponownie else R.string.moje_otworz))
        Icon(imageVector = StatusIcons.Open, contentDescription = null, modifier = Modifier.padding(start = Odstepy.s))
    }
}

@Composable
private fun StatusLine(status: ShownStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = status.icon, contentDescription = null)
        Text(
            text = stringResource(status.textRes),
            modifier = Modifier.padding(start = Odstepy.s),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun titleText(item: OwnSubmissionItem): String =
    when (item.kind) {
        SubmissionKind.ENTRANCE_MARKING -> {
            stringResource(R.string.moje_zmiana_oznaczenia)
        }

        SubmissionKind.PRECISE, SubmissionKind.QUICK -> {
            stringResource(item.type?.nameRes() ?: R.string.moje_typ_nieznany)
        }
    }

private fun outdatedText(kind: SubmissionKind): Int =
    if (kind == SubmissionKind.ENTRANCE_MARKING) R.string.moje_nieaktualne_oznaczenie else R.string.moje_nieaktualne

private const val OUTDATED_REASON = "nieaktualne"

/** Czy zgłoszenie odrzucono jako „nieaktualne”. Wymagania: BR-23, BR-24 */
internal val OwnSubmissionItem.isOutdated: Boolean
    get() = shownStatus() == ShownStatus.REJECTED && rejectionReason?.trim().equals(OUTDATED_REASON, ignoreCase = true)

/**
 * Czy pozycję można otworzyć z C3: status pozwala na poprawkę ([canOpen]), a rodzaj ma ekran poprawki. Zmiana
 * oznaczenia wejścia nie otwiera C2. TODO(#3849): zmiana oznaczenia otwiera C6.
 *
 * Wymagania: US8.1, US8.3, US17.2
 */
internal fun OwnSubmissionItem.canOpenHere(): Boolean =
    kind != SubmissionKind.ENTRANCE_MARKING && canOpen(shownStatus())

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

private fun RefusalReason.textRes(): Int =
    when (this) {
        RefusalReason.NO_PERMISSION -> R.string.moje_brak_uprawnien
        RefusalReason.NOT_ACCEPTED -> R.string.moje_nie_przyjal
        RefusalReason.UNREADABLE -> R.string.moje_nie_odczytano
        RefusalReason.CHANGED_ON_SERVER -> R.string.moje_zmienione_na_serwerze
    }
