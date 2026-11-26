/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

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
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.submission.Severity

/** Znacznik testowy znaku uciążliwości przy wartości wyboru „Uciążliwość”. */
public const val SEVERITY_SIGN_TAG: String = "znak-uciazliwosci"

/**
 * „Uciążliwość”: nagłówek, podpowiedź i trzy wybory z opisem, oceniane dla osoby, dla której bariera jest
 * najtrudniejsza.
 *
 * Wymagania: US7.1, US10.3
 *
 * @param severity wybrana uciążliwość albo `null`
 * @param onPick wybór uciążliwości
 * @param modifier położenie wyboru wyznacza wołający
 * @param isError ramki wyborów w kolorze błędu, gdy uciążliwości brakuje przy czynności
 */
@Composable
public fun SeverityChoice(
    severity: Severity?,
    onPick: (Severity) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    Column(modifier = modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(R.string.wybor_uciazliwosc),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        Text(stringResource(R.string.wybor_uciazliwosc_podpowiedz))
        Severity.entries.forEach { option ->
            SeverityOption(option = option, isChosen = option == severity, isError = isError, onPick = onPick)
        }
    }
}

@Composable
private fun SeverityOption(option: Severity, isChosen: Boolean, isError: Boolean, onPick: (Severity) -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().selectable(selected = isChosen, role = Role.RadioButton) { onPick(option) },
        border =
            BorderStroke(
                width = if (isChosen) Obramowania.wybrane else Obramowania.ramka,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Column(modifier = Modifier.padding(Odstepy.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isChosen, onClick = null)
                val sign = Modifier.padding(start = Odstepy.s).testTag(SEVERITY_SIGN_TAG)
                SeverityIcon(severity = option, modifier = sign)
                Text(
                    text = stringResource(option.nameRes()),
                    modifier = Modifier.padding(start = Odstepy.s),
                    fontWeight = Typografia.wyroznienieWaga,
                )
            }
            Text(stringResource(option.descriptionRes()))
        }
    }
}
