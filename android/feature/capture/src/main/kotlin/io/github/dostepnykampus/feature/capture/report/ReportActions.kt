/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.marking.BarrierTypeField
import io.github.dostepnykampus.core.designsystem.marking.SeverityChoice
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import io.github.dostepnykampus.feature.capture.report.form.DescriptionSection
import io.github.dostepnykampus.feature.capture.report.form.ExitDialog
import io.github.dostepnykampus.feature.capture.report.form.FloorSection
import io.github.dostepnykampus.feature.capture.report.form.MissingField
import io.github.dostepnykampus.feature.capture.report.form.PhotoSection
import io.github.dostepnykampus.feature.capture.report.form.PlaceCard
import io.github.dostepnykampus.feature.capture.report.form.PlanFloorSection
import io.github.dostepnykampus.feature.capture.report.form.ReportBanner
import io.github.dostepnykampus.feature.capture.report.form.ReportEdit
import io.github.dostepnykampus.feature.capture.report.form.ReportFormError.MISSING_BARRIER_TYPE
import io.github.dostepnykampus.feature.capture.report.form.ReportFormError.MISSING_DESCRIPTION
import io.github.dostepnykampus.feature.capture.report.form.ReportFormError.MISSING_FLOOR
import io.github.dostepnykampus.feature.capture.report.form.ReportFormError.MISSING_PHOTO
import io.github.dostepnykampus.feature.capture.report.form.ReportFormError.MISSING_SEVERITY
import io.github.dostepnykampus.feature.capture.report.form.ReportStage
import io.github.dostepnykampus.feature.capture.report.form.ReportUiState
import io.github.dostepnykampus.feature.capture.report.form.rememberMissingFields

/**
 * Czynności ekranu C2 przekazywane z ViewModelu.
 *
 * Wymagania: US7.4, US7.1
 *
 * @property onEdit zmiana pola
 * @property onChangePlace „Zmień miejsce”: powrót do C1
 * @property camera „Zrób zdjęcie”: aparat i wynik ostatniej próby
 * @property onSave „Zapisz” i „Spróbuj ponownie”
 * @property onBack „Wstecz”
 * @property onConfirmExit „Wyjdź bez zapisu”
 * @property onStay „Wróć do formularza”
 */
data class ReportActions(
    val onEdit: (ReportEdit) -> Unit,
    val onChangePlace: () -> Unit,
    val camera: PhotoCamera,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
    val onConfirmExit: () -> Unit,
    val onStay: () -> Unit,
)

/**
 * Treść C2 „Opisz barierę” dla stanu [state], bez ViewModelu. Każde „Zapisz” z brakami przewija do pierwszego
 * brakującego pola i przenosi fokus na komunikat przy nim.
 *
 * Wymagania: US7, NFR-15
 *
 * @param state stan formularza
 * @param actions czynności ekranu
 * @param modifier wymiary nadane przez aplikację
 */
@Composable
fun ReportContent(state: ReportUiState, actions: ReportActions, modifier: Modifier = Modifier) {
    val isEdit = state.form.replacesSubmissionId != null || state.form.editedSubmission != null
    val title = stringResource(if (isEdit) R.string.formularz_naglowek_edycja else R.string.formularz_naglowek)
    val errors = state.errors
    val fields = rememberMissingFields(errors = errors, attempts = state.missingAttempts)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
            BackHeader(title = title, onBack = actions.onBack)
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Odstepy.m),
                verticalArrangement = Arrangement.spacedBy(Odstepy.m),
            ) {
                ReplaceNotice(state.form.replacesSubmissionId)
                ReportBanner(state = state, onRetry = actions.onSave)
                PlaceCard(place = state.form.place, onChangePlace = actions.onChangePlace)
                state.form.place.buildingSymbol?.let { building ->
                    MissingField(error = MISSING_FLOOR, errors = errors, fields = fields) {
                        val floors = state.form.place.floors
                        if (floors.items.isEmpty()) {
                            FloorSection(building = building, floor = state.form.floor, onEdit = actions.onEdit)
                        } else {
                            PlanFloorSection(floors = floors, floor = state.form.floor, onEdit = actions.onEdit)
                        }
                    }
                }
                MissingField(error = MISSING_BARRIER_TYPE, errors = errors, fields = fields) {
                    BarrierTypeField(
                        type = state.form.barrierType,
                        onPick = { actions.onEdit(ReportEdit.Type(it)) },
                        isError = MISSING_BARRIER_TYPE in errors,
                    )
                }
                MissingField(error = MISSING_SEVERITY, errors = errors, fields = fields) {
                    SeverityChoice(
                        severity = state.form.severity,
                        onPick = { actions.onEdit(ReportEdit.Rating(it)) },
                        isError = MISSING_SEVERITY in errors,
                    )
                }
                MissingField(error = MISSING_PHOTO, errors = errors, fields = fields) {
                    PhotoSection(photos = PhotoPaths(state.photos.map { it.path }), camera = actions.camera)
                }
                MissingField(error = MISSING_DESCRIPTION, errors = errors, fields = fields) {
                    DescriptionSection(text = state.form.description, onEdit = actions.onEdit)
                }
            }
            if (state.stage == ReportStage.EXIT_ASKED) {
                ExitDialog(onConfirmExit = actions.onConfirmExit, onStay = actions.onStay)
            }
            PinnedActionBar {
                PinnedButton(text = stringResource(R.string.formularz_zapisz), onClick = actions.onSave)
            }
        }
    }
}

@Composable
private fun ReplaceNotice(replacesSubmissionId: String?) {
    if (replacesSubmissionId.isNullOrBlank()) return
    Text(
        text = stringResource(R.string.formularz_zmiana_przyjetego),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
