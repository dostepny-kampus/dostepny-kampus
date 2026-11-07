/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia

/** „Zdjęcie”: ostrzeżenie „Ważne” jak w kroku 1 V4, podgląd zdjęć i „Zrób zdjęcie” (US7.1, NFR-25, PW-8). */
@Composable
internal fun PhotoSection(photos: PhotoPaths, onTakePhoto: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_zdjecie))
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.error),
        ) {
            Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
                Text(
                    text = stringResource(R.string.formularz_wazne),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = Typografia.wyroznienieWaga,
                )
                Text(stringResource(R.string.formularz_wazne_twarze), fontWeight = Typografia.wyroznienieWaga)
                Text(stringResource(R.string.formularz_wazne_widoczne))
            }
        }
        PhotoPreviews(photos)
        val hasPhoto = photos.items.isNotEmpty()
        val label = if (hasPhoto) R.string.formularz_zrob_nowe_zdjecie else R.string.formularz_zrob_zdjecie
        FilledTonalButton(
            onClick = onTakePhoto,
            modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk),
        ) {
            Text(stringResource(label))
        }
    }
}

/** „Opis”, obowiązkowy przy TB-29 „inna bariera” (US7.3). */
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
internal fun FieldError(error: ReportFormError, isShown: Boolean) {
    if (isShown) {
        Text(
            text = stringResource(error.messageRes()),
            color = MaterialTheme.colorScheme.error,
            fontWeight = Typografia.wyroznienieWaga,
        )
    }
}

/** Nad formularzem: pierwszy brak albo „Za mało miejsca w telefonie” ze „Spróbuj ponownie” (PW-10, NFR-7). */
@Composable
internal fun ReportBanner(state: ReportUiState, onRetry: () -> Unit) {
    val message =
        if (state.stage == ReportStage.SAVE_FAILED) {
            R.string.formularz_blad_zapisu
        } else {
            state.errors.firstOrNull()?.messageRes()
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

/** „Wyjść bez zapisu?” po „Wstecz” z niezapisanymi zmianami (US7.4, NFR-23). */
@Composable
internal fun ExitDialog(onConfirmExit: () -> Unit, onStay: () -> Unit) {
    AlertDialog(
        onDismissRequest = onStay,
        title = { Text(stringResource(R.string.formularz_wyjsc_pytanie)) },
        text = { Text(stringResource(R.string.formularz_wyjsc_opis)) },
        confirmButton = { TextButton(onClick = onConfirmExit) { Text(stringResource(R.string.formularz_wyjdz)) } },
        dismissButton = { TextButton(onClick = onStay) { Text(stringResource(R.string.formularz_wroc)) } },
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
