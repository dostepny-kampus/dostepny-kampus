/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.invitation

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.StaffProblem
import io.github.dostepnykampus.core.designsystem.StaffProblemMessage
import io.github.dostepnykampus.core.designsystem.StaffResultMessage
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.inspection.R

/**
 * K2 „Zaproś osobę” ze stanem z modelu widoku.
 *
 * Wymagania: US12.1
 *
 * @param onBack powrót do konta
 * @param onLeave powrót do mapy przy braku uprawnień
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel stan zaproszenia; w aplikacji z grafu Hilt
 */
@Composable
fun InvitationScreen(
    onBack: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvitationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InvitationContent(
        state = state,
        onBack = onBack,
        onLeave = onLeave,
        onEdit = viewModel::edit,
        onChoose = viewModel::choose,
        onSend = viewModel::send,
        modifier = modifier,
    )
}

/**
 * Widok K2 bez modelu widoku: adres, rola, wynik zaproszenia, błędy danych i stany wspólne.
 * TODO(#3868): znak przed błędami pól.
 *
 * Wymagania: US12.1, US12.2, NFR-50, NFR-52
 *
 * @param state stan ekranu
 * @param onBack powrót do konta
 * @param onLeave powrót do mapy przy braku uprawnień
 * @param onEdit zmiana adresu
 * @param onChoose wybór roli
 * @param onSend „Wyślij zaproszenie” i „Spróbuj ponownie”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun InvitationContent(
    state: InvitationUiState,
    onBack: () -> Unit,
    onLeave: () -> Unit,
    onEdit: (String) -> Unit,
    onChoose: (StaffRole) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val problem = problemOf(state.stage)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.zaproszenie_naglowek), onBack = onBack)
            if (problem != null) {
                val onAction = if (problem == StaffProblem.FORBIDDEN) onLeave else onSend
                StaffProblemMessage(problem = problem, onAction = onAction)
            } else {
                InvitationBody(state = state, onEdit = onEdit, onChoose = onChoose, onSend = onSend)
            }
        }
    }
}

@Composable
private fun ColumnScope.InvitationBody(
    state: InvitationUiState,
    onEdit: (String) -> Unit,
    onChoose: (StaffRole) -> Unit,
    onSend: () -> Unit,
) {
    val isInContent = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.m),
    ) {
        Message(state)
        EmailField(
            email = state.email,
            isWrong = InvitationProblem.EMAIL in state.problems,
            onEdit = onEdit,
        )
        Roles(
            chosen = state.role,
            isMissing = InvitationProblem.ROLE in state.problems,
            onChoose = onChoose,
        )
        if (isInContent) SendButton(onSend)
    }
    if (!isInContent) PinnedActionBar { SendButton(onSend) }
}

private fun problemOf(stage: InvitationStage): StaffProblem? =
    when (stage) {
        InvitationStage.OFFLINE -> StaffProblem.OFFLINE
        InvitationStage.SERVER_ERROR -> StaffProblem.SERVER
        InvitationStage.FORBIDDEN -> StaffProblem.FORBIDDEN
        InvitationStage.IDLE, InvitationStage.SENDING, InvitationStage.SENT, InvitationStage.ROLE_ADDED -> null
    }

@Composable
private fun SendButton(onSend: () -> Unit) {
    PinnedButton(
        text = stringResource(R.string.zaproszenie_wyslij),
        onClick = onSend,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Message(state: InvitationUiState) {
    val title =
        when {
            state.stage == InvitationStage.SENT -> R.string.zaproszenie_wyslane
            state.stage == InvitationStage.ROLE_ADDED -> R.string.zaproszenie_konto_istnieje
            else -> return
        }
    val role = state.addedRole
    val message =
        if (state.stage == InvitationStage.ROLE_ADDED && role != null) {
            stringResource(R.string.zaproszenie_rola_dodana, stringResource(genitiveOf(role)))
        } else {
            null
        }
    StaffResultMessage(title = stringResource(title), message = message)
}

@Composable
private fun EmailField(email: String, isWrong: Boolean, onEdit: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(R.string.zaproszenie_adres)
        if (isWrong) ProblemText(R.string.zaproszenie_adres_niepelny)
        OutlinedTextField(
            value = email,
            onValueChange = onEdit,
            modifier = Modifier.fillMaxWidth(),
            isError = isWrong,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
    }
}

@Composable
private fun Roles(chosen: StaffRole?, isMissing: Boolean, onChoose: (StaffRole) -> Unit) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(R.string.zaproszenie_rola)
        if (isMissing) ProblemText(R.string.zaproszenie_wybierz_role)
        StaffRole.entries.forEach { role ->
            RoleOption(role = role, isChosen = role == chosen, isMissing = isMissing) { onChoose(role) }
        }
    }
}

@Composable
private fun SectionTitle(
    @StringRes text: Int,
) {
    Text(
        text = stringResource(text),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
private fun ProblemText(
    @StringRes text: Int,
) {
    Text(
        text = stringResource(text),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
        color = MaterialTheme.colorScheme.error,
        fontWeight = Typografia.wyroznienieWaga,
    )
}

@Composable
private fun RoleOption(role: StaffRole, isChosen: Boolean, isMissing: Boolean, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().selectable(selected = isChosen, role = Role.RadioButton, onClick = onClick),
        border =
            BorderStroke(
                width = if (isChosen) Obramowania.wybrane else Obramowania.ramka,
                color = if (isMissing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
            ),
    ) {
        Row(modifier = Modifier.padding(Odstepy.m), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = isChosen, onClick = null)
            Text(
                text = stringResource(nameOf(role)),
                modifier = Modifier.padding(start = Odstepy.s),
                fontWeight = Typografia.wyroznienieWaga,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
