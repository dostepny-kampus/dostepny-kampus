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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.review.SimilarSubmission
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
 * Czynności decyzji R2.
 *
 * @property onAccept „Przyjmij”
 * @property onReject „Odrzuć” z wybranym powodem
 */
internal data class DecisionActions(val onAccept: () -> Unit, val onReject: (Rejection) -> Unit)

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
    similar: List<SimilarSubmission>,
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
        Button(onClick = actions.onAccept, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sprawdzenie_przyjmij))
        }
        Text(
            text = stringResource(R.string.sprawdzenie_powod),
            modifier = Modifier.padding(top = Odstepy.m).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            if (allowsDuplicate) Choice(duplicate, reason == Reason.DUPLICATE) { reason = Reason.DUPLICATE }
            if (reason == Reason.DUPLICATE) Earlier(similar = similar, chosen = earlier) { earlier = it }
            Choice(outdated, reason == Reason.OUTDATED) { reason = Reason.OUTDATED }
            Choice(stringResource(R.string.sprawdzenie_inny), reason == Reason.OTHER) { reason = Reason.OTHER }
        }
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.sprawdzenie_opisz)) },
        )
        if (notice == CheckNotice.REASON_MISSING) Text(stringResource(R.string.sprawdzenie_podaj_powod))
        OutlinedButton(
            onClick = {
                val rejection =
                    when (reason) {
                        Reason.DUPLICATE -> Rejection(reason = duplicate, duplicateOf = earlier)
                        Reason.OUTDATED -> Rejection(reason = outdated)
                        Reason.OTHER -> Rejection(reason = text)
                        null -> Rejection(reason = "")
                    }
                actions.onReject(rejection)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.sprawdzenie_odrzuc))
        }
    }
}

@Composable
private fun Earlier(similar: List<SimilarSubmission>, chosen: String?, onChoose: (String) -> Unit) {
    Column(modifier = Modifier.padding(start = Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.sprawdzenie_wczesniejsze))
        similar.forEach { near ->
            val label =
                stringResource(
                    R.string.sprawdzenie_wczesniejsze_pozycja,
                    stringResource(near.submission.content.titleRes()),
                    near.distanceMeters.roundToInt(),
                )
            Choice(label, chosen == near.submission.id) { onChoose(near.submission.id) }
        }
    }
}

@Composable
private fun Choice(label: String, isChosen: Boolean, onChoose: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().selectable(selected = isChosen, role = Role.RadioButton, onClick = onChoose),
        border =
            BorderStroke(
                width = if (isChosen) Obramowania.wybrane else Obramowania.ramka,
                color = MaterialTheme.colorScheme.outline,
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
 * Komunikat w ramce: zdanie pogrubione i zdanie pod nim.
 *
 * @param title pierwsze zdanie
 * @param body drugie zdanie
 */
@Composable
internal fun Notice(@StringRes title: Int, @StringRes body: Int) {
    GroupFrame {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}
