/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.ProblemText
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.card.TextAction
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import io.github.dostepnykampus.feature.capture.photo.PhotoPreviews
import io.github.dostepnykampus.feature.capture.photo.PhotoWarning

/**
 * „Zdjęcie”: ostrzeżenie „Ważne” jak w kroku 1 V4, podgląd zdjęć, „Zrób zdjęcie” i komunikat, gdy zdjęcia nie dało
 * się zapisać (PW-8).
 *
 * Wymagania: US7.1, NFR-25, NFR-50
 *
 * @param photos zrobione zdjęcia do podglądu
 * @param camera aparat „Zrób zdjęcie”
 * @param title nagłówek sekcji; `null` daje sekcję bez nagłówka
 * @param isTakeShown czy „Zrób zdjęcie” stoi w sekcji; bez niego ekran stawia przycisk sam
 * @param validation treść komunikatu walidacji nad „Zrób zdjęcie”
 */
@Composable
internal fun PhotoSection(
    photos: PhotoPaths,
    camera: PhotoCamera,
    title: String? = stringResource(R.string.formularz_zdjecie),
    isTakeShown: Boolean = true,
    validation: @Composable () -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        title?.let { SectionTitle(it) }
        PhotoWarning {
            Text(stringResource(R.string.formularz_wazne_twarze), fontWeight = Typografia.wyroznienieWaga)
            Text(stringResource(R.string.formularz_wazne_widoczne))
        }
        PhotoPreviews(photos)
        validation()
        if (isTakeShown) {
            FilledTonalButton(
                onClick = camera.onTake,
                modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk),
            ) {
                Text(takeLabel(photos))
            }
        }
        if (camera.isFailed) {
            Text(
                text = stringResource(R.string.formularz_zdjecie_blad),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.error,
                fontWeight = Typografia.wyroznienieWaga,
            )
        }
    }
}

/** „Zrób zdjęcie”, a gdy zdjęcie już jest, „Zrób nowe zdjęcie”. Wymagania: US7.1, US4.1 */
@Composable
@ReadOnlyComposable
internal fun takeLabel(photos: PhotoPaths): String {
    val label = if (photos.items.isEmpty()) R.string.formularz_zrob_zdjecie else R.string.formularz_zrob_nowe_zdjecie
    return stringResource(label)
}

/** „Opis”, obowiązkowy przy TB-29 „inna bariera”. Wymagania: US7.3 */
@Composable
internal fun DescriptionSection(text: String, onEdit: (ReportEdit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_opis))
        Text(stringResource(R.string.formularz_opis_podpowiedz))
        OutlinedTextField(
            value = text,
            onValueChange = { onEdit(ReportEdit.Description(it.take(DESCRIPTION_MAX_LENGTH))) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )
    }
}

/** Ten sam tekst braku przy polu, co nad formularzem (PW-10), gdy [isShown]. */
@Composable
internal fun FieldError(error: ReportFormError, isShown: Boolean, modifier: Modifier = Modifier) {
    if (isShown) ProblemText(text = stringResource(error.messageRes()), modifier = modifier)
}

/**
 * Nad formularzem: pierwszy brak, „Za mało miejsca w telefonie” ze „Spróbuj ponownie” albo odrzucony zapis
 * (PW-10).
 *
 * Wymagania: NFR-7, NFR-50
 */
@Composable
internal fun ReportBanner(state: ReportUiState, onRetry: () -> Unit) {
    val message =
        when (state.stage) {
            ReportStage.SAVE_FAILED -> R.string.formularz_blad_zapisu

            ReportStage.SAVE_REJECTED -> R.string.formularz_zapis_odrzucony

            ReportStage.VERSION_UNKNOWN -> R.string.formularz_wersja_nieznana

            ReportStage.EDITING,
            ReportStage.SAVING,
            ReportStage.EXIT_ASKED,
            ReportStage.SAVED,
            ReportStage.LEFT,
            ReportStage.PLACE_CHANGED,
            -> state.errors.firstOrNull()?.messageRes()
        } ?: return
    Column(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        Text(
            text = stringResource(message),
            color = MaterialTheme.colorScheme.error,
            fontWeight = Typografia.wyroznienieWaga,
        )
        if (state.stage == ReportStage.SAVE_FAILED) {
            FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.formularz_sprobuj_ponownie)) }
        }
    }
}

/**
 * Błąd zapisu w telefonie z ekranu wysyłanego do sprawdzenia: „Za mało miejsca w telefonie” z „Spróbuj ponownie”
 * albo odmowa kolejki ([isRejected]) z tym samym przyciskiem.
 *
 * Wymagania: NFR-7, NFR-50
 */
@Composable
internal fun SendFailure(isRejected: Boolean, onRetry: () -> Unit) {
    val message = if (isRejected) R.string.formularz_zapis_odrzucony else R.string.formularz_blad_zapisu
    Column(
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive },
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        Text(stringResource(message), color = MaterialTheme.colorScheme.error, fontWeight = Typografia.wyroznienieWaga)
        FilledTonalButton(onClick = onRetry) { Text(stringResource(R.string.formularz_sprobuj_ponownie)) }
    }
}

/** „Wyjść bez zapisu?” po „Wstecz” z niezapisanymi zmianami. Wymagania: US7.4, NFR-23 */
@Composable
internal fun ExitDialog(onConfirmExit: () -> Unit, onStay: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text(stringResource(R.string.formularz_wyjsc_pytanie)) },
        text = { Text(stringResource(R.string.formularz_wyjsc_opis)) },
        confirmButton = { TextAction(onClick = onConfirmExit) { Text(stringResource(R.string.formularz_wyjdz)) } },
        dismissButton = { TextAction(onClick = onStay) { Text(stringResource(R.string.formularz_wroc)) } },
    )
}

/** Najdłuższy opis, który przyjmie serwer (`maxLength` w api/openapi.yaml); dłuższy byłby odrzucony. */
internal const val DESCRIPTION_MAX_LENGTH = 2000

@StringRes
private fun ReportFormError.messageRes(): Int =
    when (this) {
        ReportFormError.MISSING_FLOOR -> R.string.formularz_brak_pietra
        ReportFormError.MISSING_BARRIER_TYPE -> R.string.formularz_brak_typu
        ReportFormError.MISSING_SEVERITY -> R.string.formularz_brak_uciazliwosci
        ReportFormError.MISSING_PHOTO -> R.string.formularz_brak_zdjecia
        ReportFormError.MISSING_DESCRIPTION -> R.string.formularz_brak_opisu
    }
