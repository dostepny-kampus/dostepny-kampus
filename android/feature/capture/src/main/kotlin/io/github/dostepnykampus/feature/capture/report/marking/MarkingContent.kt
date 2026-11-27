/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.GroupFrame
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.ProblemText
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.marking.AssessmentLine
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import io.github.dostepnykampus.feature.capture.report.form.PhotoSection
import io.github.dostepnykampus.feature.capture.report.form.SectionTitle
import io.github.dostepnykampus.feature.capture.report.form.SendFailure

private val Choices = listOf(EntranceMarking.ACCESSIBLE, EntranceMarking.INACCESSIBLE)

/**
 * Treść C6 „Zmiana oznaczenia wejścia” dla stanu [state], bez ViewModelu: obecne oznaczenie wejścia, wybór nowego,
 * zdjęcie wejścia, błędy walidacji i zapisu oraz przypięte „Wyślij do sprawdzenia”.
 *
 * Wymagania: US17.2, US17.3, BR-24, BR-7, NFR-7, NFR-21, NFR-25, NFR-52
 *
 * @param state stan ekranu
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun MarkingContent(state: MarkingUiState, actions: MarkingActions, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.zmiana_oznaczenia_naglowek), onBack = actions.onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.m),
            ) {
                val isRejected = state.stage == MarkingStage.SAVE_REJECTED
                if (isRejected || state.stage == MarkingStage.SAVE_FAILED) SendFailure(isRejected, actions.onSend)
                state.entrance?.let { CurrentMarking(entrance = it, buildingName = state.buildingName) }
                NewMarking(state = state, onChoose = actions.onChoose)
                PhotoSection(
                    photos = PhotoPaths(listOfNotNull(state.photo?.path)),
                    camera = actions.camera,
                    title = stringResource(R.string.zmiana_oznaczenia_zdjecie),
                ) {
                    if (state.isPhotoMissing) ProblemText(stringResource(R.string.zmiana_oznaczenia_brak_zdjecia))
                }
                Text(stringResource(R.string.zmiana_oznaczenia_sprawdzi))
            }
            PinnedActionBar {
                PinnedButton(text = stringResource(R.string.usuniecie_wyslij), onClick = actions.onSend)
            }
        }
    }
}

@Composable
private fun CurrentMarking(entrance: Entrance, buildingName: String?) {
    GroupFrame {
        Column(
            modifier = Modifier.padding(vertical = Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            Text(
                text = stringResource(R.string.zmiana_oznaczenia_obecne),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MarkingSign(marking = entrance.marking)
            entrance.assessment?.let { AssessmentLine(it) }
            Text(
                text = entrance.name ?: stringResource(R.string.zmiana_oznaczenia_bez_nazwy),
                fontWeight = Typografia.wyroznienieWaga,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(barrierPlace(symbol = entrance.buildingSymbol, name = buildingName, entrance = null, floor = null))
        }
    }
}

@Composable
private fun NewMarking(state: MarkingUiState, onChoose: (EntranceMarking) -> Unit) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.zmiana_oznaczenia_nowe))
        if (state.isChoiceMissing) {
            ProblemText(stringResource(R.string.zmiana_oznaczenia_wybierz))
        }
        Choices.forEach { option ->
            val isChosen = option == state.chosen
            MarkingOption(
                option = option,
                isChosen = isChosen,
                isConfirming = isChosen && state.isConfirming,
                isError = state.isChoiceMissing,
            ) {
                onChoose(option)
            }
        }
    }
}

@Composable
private fun MarkingOption(
    option: EntranceMarking,
    isChosen: Boolean,
    isConfirming: Boolean,
    isError: Boolean,
    onChoose: () -> Unit,
) {
    OutlinedCard(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.przycisk)
                .selectable(selected = isChosen, role = Role.RadioButton, onClick = onChoose),
        border =
            BorderStroke(
                width = if (isChosen) Obramowania.wybrane else Obramowania.ramka,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = isChosen, onClick = null)
                MarkingSign(marking = option, modifier = Modifier.padding(start = Odstepy.s))
            }
            if (isConfirming) Text(stringResource(R.string.zmiana_oznaczenia_potwierdzasz))
        }
    }
}
