/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.data.submission.own.RefusalReason
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StatusLine
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.core.designsystem.card.CardAction
import io.github.dostepnykampus.core.designsystem.card.ListItemCard
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.review.RejectionReasons
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.feature.capture.R
import java.time.ZoneId

/**
 * Pozycja listy C3: status słowem z ikoną na początku, więc TalkBack czyta go przed typem bariery,
 * potem „Bariery już nie ma” przy usunięciu bariery, rodzaj albo typ, „Nowe oznaczenie:” przy zmianie oznaczenia
 * wejścia, budynek z piętrem, data i powód przy „Nie wysłano”. Odrzucone jako „nieaktualne” ma zdanie o nowszej
 * wersji, a zgłoszenie z pomiaru zamiast „Otwórz” ma „Wyślij ponownie”; usunięcie bariery ma
 * „Zmierz i zgłoś barierę”.
 *
 * Wymagania: US8.1, NFR-18, NFR-21, US9.5, US8.3, US8.4, US17.2, BR-23, US9.3
 *
 * @param item zgłoszenie
 * @param onOpen „Otwórz” albo „Wyślij ponownie”: poprawka w C2 dla zgłoszeń, które można poprawić; `null` bez
 *   przycisku
 * @param onMeasure „Zmierz i zgłoś barierę” po odrzuceniu usunięcia bariery jako „nieaktualne”
 * @param onConflict wybór wersji w C4 przy konflikcie wersji; `null` bez przycisku
 * @param modifier wymiary nadane przez listę
 */
@Composable
internal fun SubmissionCard(
    item: OwnSubmissionItem,
    onOpen: (() -> Unit)?,
    onMeasure: () -> Unit,
    onConflict: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val status = item.shownStatus()
    ListItemCard(modifier = modifier) {
        SubmissionDetails(item = item, status = status)
        onConflict?.let { choose ->
            CardAction(text = stringResource(R.string.moje_dwie_wersje), onClick = choose, opensScreen = true)
        }
        if (item.isOutdated && item.isBarrierRemoved) {
            CardAction(text = stringResource(R.string.pomiar_naglowek), onClick = onMeasure)
        } else if (onOpen != null && canOpen(status)) {
            val open = if (item.isOutdated) R.string.moje_wyslij_ponownie else R.string.moje_otworz
            CardAction(text = stringResource(open), onClick = onOpen, opensScreen = true)
        }
    }
}

@Composable
private fun SubmissionDetails(item: OwnSubmissionItem, status: ShownStatus) {
    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
    ) {
        StatusLine(text = stringResource(status.textRes), icon = status.icon)
        if (item.isChangeWaiting) Text(stringResource(R.string.moje_zmiana_czeka))
        if (item.isBarrierRemoved) Text(stringResource(R.string.moje_bariery_juz_nie_ma))
        Text(text = titleText(item), style = MaterialTheme.typography.titleMedium)
        item.entranceMarking?.let { marking -> NewMarking(marking) }
        Text(placeText(item))
        Text(campusDay(item.savedAt.atZone(ZoneId.systemDefault()).toLocalDate()))
        item.refusal?.textRes()?.let { reason -> Text(stringResource(reason)) }
        item.rejectionReason?.takeIf { status == ShownStatus.REJECTED }?.let { reason ->
            Text(stringResource(R.string.moje_powod_odrzucenia, reason))
        }
        if (item.isOutdated) Text(stringResource(outdatedText(item)))
    }
}

@Composable
private fun NewMarking(marking: EntranceMarking) {
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.moje_nowe_oznaczenie))
        MarkingSign(marking)
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

private fun outdatedText(item: OwnSubmissionItem): Int =
    when {
        item.kind == SubmissionKind.ENTRANCE_MARKING -> R.string.moje_nieaktualne_oznaczenie
        item.isBarrierRemoved -> R.string.moje_nieaktualne_usuniecie
        else -> R.string.moje_nieaktualne
    }

/** Czy zgłoszenie czeka w telefonie na wybór jednej z dwóch wersji. Wymagania: US9.3, BR-10 */
internal val OwnSubmissionItem.hasVersionConflict: Boolean
    get() = refusal == RefusalReason.CHANGED_ON_SERVER

/** Czy zgłoszenie odrzucono jako „nieaktualne”. Wymagania: BR-23, BR-24 */
internal val OwnSubmissionItem.isOutdated: Boolean
    get() = shownStatus() == ShownStatus.REJECTED && RejectionReasons.isOutdated(rejectionReason)

/**
 * Czy pozycję można otworzyć z C3: status pozwala na poprawkę ([canOpen]). Każdy rodzaj ma ekran poprawki: C2,
 * C6 przy zmianie oznaczenia wejścia i C7 przy „bariera usunięta”.
 *
 * Wymagania: US8.1, US8.3, US8.4, US17.2
 */
internal fun OwnSubmissionItem.canOpenHere(): Boolean = canOpen(shownStatus())

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

private fun RefusalReason.textRes(): Int? =
    when (this) {
        RefusalReason.NO_PERMISSION -> R.string.moje_brak_uprawnien
        RefusalReason.NOT_ACCEPTED -> R.string.moje_nie_przyjal
        RefusalReason.UNREADABLE -> R.string.moje_nie_odczytano
        RefusalReason.CHANGED_ON_SERVER -> null
    }
