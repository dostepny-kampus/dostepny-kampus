/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.ProblemText
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.core.designsystem.card.PrimaryAction
import io.github.dostepnykampus.core.model.review.RejectionReasons
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.review.R
import io.github.dostepnykampus.feature.review.queue.titleRes
import kotlin.math.roundToInt

private enum class Reason { DUPLICATE, OUTDATED, OTHER }

/**
 * Odrzucenie wybrane na R2.
 *
 * @property reason powód dla autora; pusty, gdy Weryfikator go nie podał
 * @property duplicateOf wcześniejsze zgłoszenie przy „Duplikat”; `null` przy innym powodzie
 */
internal data class Rejection(val reason: String, val duplicateOf: String? = null)

/**
 * Czynności przyjęcia i odrzucenia zgłoszenia.
 *
 * @property onAccept „Przyjmij”
 * @property onReject „Odrzuć” z wybranym powodem
 * @property onType typ bariery wybrany dla zgłoszenia szybkiego
 * @property onSeverity uciążliwość wybrana dla zgłoszenia szybkiego
 */
internal data class DecisionActions(
    val onAccept: () -> Unit,
    val onReject: (Rejection) -> Unit,
    val onType: (BarrierType) -> Unit = {},
    val onSeverity: (Severity) -> Unit = {},
)

/**
 * Zrzut podobnych zgłoszeń do pokazania i do wyboru duplikatu.
 *
 * @param items podobne zgłoszenia od najbliższego
 * @param photos adres pierwszego zdjęcia podobnego zgłoszenia według jego identyfikatora
 */
@Immutable
internal class SimilarReports(items: List<SimilarSubmission>, photos: Map<String, String> = emptyMap()) {
    val items = items.toList()
    val photos = photos.toMap()
}

/**
 * „Przyjmij” i „Powód odrzucenia” z „Odrzuć” na końcu treści R2, z komunikatem [notice] w miejscu czynności.
 *
 * Wymagania: US10.2, BR-13, BR-16, BR-24, NFR-18
 *
 * @param similar podobne zgłoszenia do wskazania przy „Duplikat”
 * @param allowsDuplicate czy „Duplikat” jest wśród powodów; bez niego przy zmianie oznaczenia wejścia
 * @param notice komunikat o brakującym powodzie albo braku sieci; `null` bez komunikatu
 * @param actions czynności decyzji
 */
@Composable
internal fun Decision(
    similar: SimilarReports,
    allowsDuplicate: Boolean,
    notice: CheckNotice?,
    actions: DecisionActions,
) {
    var reason by rememberSaveable { mutableStateOf<Reason?>(null) }
    var earlier by rememberSaveable { mutableStateOf<String?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    val duplicate = stringResource(R.string.sprawdzenie_duplikat)
    val outdated = stringResource(R.string.sprawdzenie_nieaktualne)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        if (notice == CheckNotice.OFFLINE) {
            Notice(title = R.string.sprawdzenie_bez_sieci, body = R.string.sprawdzenie_bez_sieci_ponownie)
        }
        if (notice == CheckNotice.TYPE_MISSING) ProblemText(text = stringResource(R.string.sprawdzenie_ustal_typ))
        PrimaryAction(onClick = actions.onAccept, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sprawdzenie_przyjmij))
        }
        Text(
            text = stringResource(R.string.sprawdzenie_powod),
            modifier = Modifier.padding(top = Odstepy.m).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        val isMissing = notice == CheckNotice.REASON_MISSING
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            if (allowsDuplicate) {
                Choice(label = duplicate, isChosen = reason == Reason.DUPLICATE, isError = isMissing) {
                    reason = Reason.DUPLICATE
                }
            }
            if (reason == Reason.DUPLICATE) Earlier(similar = similar, chosen = earlier) { earlier = it }
            Choice(label = outdated, isChosen = reason == Reason.OUTDATED, isError = isMissing) {
                reason = Reason.OUTDATED
            }
            val other = stringResource(R.string.sprawdzenie_inny)
            Choice(label = other, isChosen = reason == Reason.OTHER, isError = isMissing) { reason = Reason.OTHER }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.sprawdzenie_opisz)) },
            isError = isMissing && reason == Reason.OTHER,
        )
        if (isMissing) ProblemText(text = stringResource(R.string.sprawdzenie_podaj_powod))
        OutlinedAction(
            onClick = {
                actions.onReject(
                    rejectionOf(reason = reason, duplicate = duplicate, earlier = earlier, text = text),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sprawdzenie_odrzuc))
        }
    }
}

private fun rejectionOf(reason: Reason?, duplicate: String, earlier: String?, text: String): Rejection =
    when (reason) {
        Reason.DUPLICATE -> Rejection(reason = duplicate, duplicateOf = earlier)
        Reason.OUTDATED -> Rejection(reason = RejectionReasons.OUTDATED)
        Reason.OTHER -> Rejection(reason = text)
        null -> Rejection(reason = "")
    }

@Composable
private fun Earlier(similar: SimilarReports, chosen: String?, onChoose: (String) -> Unit) {
    Column(modifier = Modifier.padding(start = Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.sprawdzenie_wczesniejsze))
        similar.items.forEach { near ->
            val label =
                stringResource(
                    R.string.sprawdzenie_wczesniejsze_pozycja,
                    stringResource(near.submission.content.titleRes()),
                    near.distanceMeters.roundToInt(),
                )
            Choice(label = label, isChosen = chosen == near.submission.id, isError = false) {
                onChoose(near.submission.id)
            }
        }
    }
}

@Composable
private fun Choice(label: String, isChosen: Boolean, isError: Boolean, onChoose: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().selectable(selected = isChosen, role = Role.RadioButton, onClick = onChoose),
        border =
            BorderStroke(
                width = if (isChosen) Obramowania.wybrane else Obramowania.ramka,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Row(modifier = Modifier.padding(Odstepy.m), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isChosen, onClick = null)
            Text(
                text = label,
                modifier = Modifier.padding(start = Odstepy.s),
                fontWeight = Typografia.wyroznienieWaga,
            )
        }
    }
}

/**
 * Komunikat w ramce: zdanie pogrubione, zdanie pod nim i opcjonalny wiersz szczegółów.
 *
 * @param title pierwsze zdanie
 * @param body drugie zdanie
 * @param detail wiersz pod zdaniami; `null` bez wiersza
 */
@Composable
internal fun Notice(
    @StringRes title: Int,
    @StringRes body: Int,
    detail: String? = null,
) {
    GroupFrame {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodyLarge)
    }
}
