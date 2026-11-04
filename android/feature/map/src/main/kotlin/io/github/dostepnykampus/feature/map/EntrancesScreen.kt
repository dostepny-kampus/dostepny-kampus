/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.dostepnykampus.core.designsystem.BackHeader
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.MarkingChip
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.core.designsystem.PinnedButtonKind
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.EntranceMarking

/**
 * V3 „Którędy wejść” (US3, BR-25): jedno zdanie i tylko wejścia dostępne, zalecane pierwsze.
 * „Pokaż wejście na mapie” zaznacza to wejście na V1. Nieznany symbol: jak karta, „budynku nie ma w telefonie”.
 *
 * @param symbol symbol z trasy, także w nagłówku w trakcie wczytywania
 * @param onShowEntrance „Pokaż wejście na mapie” dla identyfikatora wejścia
 * @param onBack „Wstecz” do karty budynku
 * @param modifier wymiary nadane przez aplikację
 * @param viewModel ten sam stan co karta; w aplikacji z grafu Hilt, w testach z podróbką katalogu
 */
@Composable
fun EntrancesScreen(
    symbol: String,
    onShowEntrance: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BuildingCardViewModel =
        hiltViewModel<BuildingCardViewModel, BuildingCardViewModel.Factory>(
            creationCallback = { factory -> factory.create(symbol) },
        ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val card = state
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = stringResource(R.string.wizyta_naglowek, symbol), onBack = onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.s),
            ) {
                when (card) {
                    BuildingCardUiState.Loading -> Unit
                    BuildingCardUiState.Missing -> MissingBuilding(onRetry = viewModel::retry)
                    is BuildingCardUiState.Ready -> VisitBody(card = card, onShowEntrance = onShowEntrance)
                }
            }
        }
    }
}

@Composable
private fun VisitBody(card: BuildingCardUiState.Ready, onShowEntrance: (String) -> Unit) {
    val accessible = card.entrances.filter { it.entrance.marking == EntranceMarking.ACCESSIBLE }
    val recommended = accessible.firstOrNull { it.isRecommended }
    val others = accessible.filterNot { it.isRecommended }
    val common = commonAssessment(accessible)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = answerSentence(symbol = card.symbol, answer = visitAnswer(card.entrances)),
            style = MaterialTheme.typography.bodyLarge,
        )
        common?.let { AssessmentLine(it) }
        if (recommended != null) {
            GroupContainer(title = stringResource(R.string.wizyta_zalecane)) {
                AccessibleEntrance(recommended, withAssessment = common == null, onShowEntrance = onShowEntrance)
            }
        }
        if (others.isNotEmpty()) {
            GroupContainer(title = stringResource(R.string.wizyta_inne)) {
                others.forEach {
                    AccessibleEntrance(it, withAssessment = common == null, onShowEntrance = onShowEntrance)
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun answerSentence(symbol: String, answer: VisitAnswer): String {
    val unnamed = stringResource(R.string.karta_bez_nazwy)
    return when (answer) {
        VisitAnswer.NoData -> stringResource(R.string.wizyta_brak_danych, symbol)
        is VisitAnswer.Accessible -> stringResource(R.string.wizyta_dostepne, symbol, answer.name ?: unnamed)
        VisitAnswer.NoAccessible -> stringResource(R.string.wizyta_brak, symbol)
        is VisitAnswer.Unknown -> stringResource(R.string.wizyta_nie_wiemy, symbol, answer.missing, answer.total)
    }
}

@Composable
private fun AccessibleEntrance(listed: OrderedEntrance, withAssessment: Boolean, onShowEntrance: (String) -> Unit) {
    val name = listed.entrance.name ?: stringResource(R.string.karta_bez_nazwy)
    val spoken = stringResource(R.string.wizyta_pokaz_czytaj, name)
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
        ) {
            MarkingChip(marking = listed.entrance.marking)
            if (listed.isRecommended) {
                Text(
                    text = stringResource(R.string.karta_zalecane),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = Typografia.wyroznienieWaga,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Text(text = name, fontWeight = Typografia.wyroznienieWaga, style = MaterialTheme.typography.titleMedium)
        if (withAssessment) listed.entrance.assessment?.let { AssessmentLine(it) }
        PinnedButton(
            text = stringResource(R.string.wizyta_pokaz),
            onClick = { onShowEntrance(listed.entrance.id) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = spoken },
            kind = PinnedButtonKind.Tonal,
        )
    }
}
