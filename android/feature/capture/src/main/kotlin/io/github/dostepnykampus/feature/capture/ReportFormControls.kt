/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Wywołania modelu widoku C2, które spina [ReportFormHost]; składa je [controls].
 *
 * @property uiState stan formularza; `null`, dopóki ekran go nie otworzy
 * @property onEdit zmiana pola
 * @property onChangePlace „Zmień miejsce”
 * @property onSave „Zapisz”
 * @property onBack „Wstecz” w nagłówku i wstecz telefonu
 * @property onConfirmExit „Wyjdź bez zapisu”
 * @property onStay „Wróć do formularza”
 */
internal data class ReportFormControls(
    val uiState: StateFlow<ReportUiState?>,
    val onEdit: (ReportEdit) -> Unit,
    val onChangePlace: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
    val onConfirmExit: () -> Unit,
    val onStay: () -> Unit,
)

/** Wywołania formularza [this] dla [ReportFormHost]. */
internal fun ReportFormViewModel.controls(): ReportFormControls =
    ReportFormControls(
        uiState = uiState,
        onEdit = ::edit,
        onChangePlace = ::changePlace,
        onSave = ::save,
        onBack = ::back,
        onConfirmExit = ::confirmExit,
        onStay = ::stay,
    )

/**
 * Formularz C2 spięty z modelem widoku, wspólny dla nowego zgłoszenia i poprawki z C3 (US7, US8.3). Każdy etap jest
 * obsługiwany raz, więc wyjście i zapis zdejmują ekran z historii tylko raz. „Wstecz” telefonu działa jak „Wstecz”
 * w nagłówku (US7.4).
 *
 * @param controls wywołania modelu widoku
 * @param camera aparat zdjęć formularza
 * @param exits wyjścia: bez zapisu, po zapisie i po „Zmień miejsce”
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
internal fun ReportFormHost(
    controls: ReportFormControls,
    camera: PhotoCamera,
    exits: ReportEditExits,
    modifier: Modifier = Modifier,
) {
    val state by controls.uiState.collectAsStateWithLifecycle()
    val leave by rememberUpdatedState(exits.onLeave)
    val save by rememberUpdatedState(exits.onSave)
    val changePlace by rememberUpdatedState(exits.onChangePlace)
    LaunchedEffect(controls.uiState) {
        controls.uiState
            .map { it?.stage }
            .distinctUntilChanged()
            .collect { stage ->
                if (stage == ReportStage.LEFT) leave()
                if (stage == ReportStage.PLACE_CHANGED) changePlace()
                if (stage == ReportStage.SAVED) save()
            }
    }
    BackHandler(onBack = controls.onBack)
    state?.let { current ->
        val actions =
            ReportActions(
                onEdit = controls.onEdit,
                onChangePlace = controls.onChangePlace,
                camera = camera,
                onSave = controls.onSave,
                onBack = controls.onBack,
                onConfirmExit = controls.onConfirmExit,
                onStay = controls.onStay,
            )
        ReportContent(state = current, actions = actions, modifier = modifier)
    }
}
