/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.PinnedButton

/**
 * Czynności ekranu C2 przekazywane z ViewModelu.
 *
 * @property onEdit zmiana pola
 * @property onChangePlace „Zmień miejsce”: powrót do C1 (US7.4)
 * @property onSave „Zapisz”
 * @property onBack „Wstecz”
 */
data class ReportActions(
    val onEdit: (ReportEdit) -> Unit,
    val onChangePlace: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
)

/**
 * Treść C2 „Opisz barierę” dla stanu [state], bez ViewModelu (US7).
 *
 * @param state stan formularza
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun ReportContent(state: ReportUiState, actions: ReportActions, modifier: Modifier = Modifier) {
    val isEdit = state.form.replacesSubmissionId != null
    val title = stringResource(if (isEdit) R.string.formularz_naglowek_edycja else R.string.formularz_naglowek)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = title, onBack = actions.onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.m),
            ) {
                PlaceCard(place = state.form.place, onChangePlace = actions.onChangePlace)
                SeveritySection(severity = state.form.severity, onEdit = actions.onEdit)
            }
            PinnedActionBar {
                PinnedButton(text = stringResource(R.string.formularz_zapisz), onClick = actions.onSave)
            }
        }
    }
}
