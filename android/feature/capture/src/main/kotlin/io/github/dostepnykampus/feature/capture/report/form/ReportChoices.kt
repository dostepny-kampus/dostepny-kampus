/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import io.github.dostepnykampus.core.designsystem.FloorOption
import io.github.dostepnykampus.core.designsystem.FloorOptions
import io.github.dostepnykampus.core.designsystem.FloorPicker
import io.github.dostepnykampus.core.designsystem.FloorPickerLayout
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.capture.R

/**
 * Piętro wpisane liczbą w budynku bez planu PWr, z polem „Półpiętro” (PW-6).
 * Wpisany tekst należy do budynku: po „Zmień miejsce” na inny budynek pole startuje od piętra ze stanu.
 *
 * Wymagania: US7.2, BR-G1
 */
@Composable
internal fun FloorSection(building: String, floor: ReportFloor?, onEdit: (ReportEdit) -> Unit) {
    val numbered = floor as? ReportFloor.Numbered
    var text by rememberSaveable(building) { mutableStateOf(numbered?.run { number.toString() }.orEmpty()) }
    val isMezzanine = numbered?.isMezzanine == true
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_pietro))
        Text(stringResource(R.string.formularz_pietro_bez_planu))
        val isWrong = text.isNotBlank() && parseFloorNumber(text) == null
        val wrongHint: (@Composable () -> Unit)? =
            if (isWrong) {
                { Text(stringResource(R.string.formularz_numer_pietra_blad)) }
            } else {
                null
            }
        OutlinedTextField(
            value = text,
            onValueChange = { value ->
                text = value
                val number = parseFloorNumber(value)
                onEdit(ReportEdit.Floor(number?.let { ReportFloor.Numbered(number = it, isMezzanine = isMezzanine) }))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.formularz_numer_pietra)) },
            isError = isWrong,
            supportingText = wrongHint,
            singleLine = true,
        )
        Row(
            modifier =
                Modifier.toggleable(value = isMezzanine, enabled = numbered != null, role = Role.Checkbox) {
                    onEdit(ReportEdit.Floor(numbered?.copy(isMezzanine = it)))
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = isMezzanine, onCheckedChange = null, enabled = numbered != null)
            Text(stringResource(R.string.formularz_polpietro))
        }
    }
}

/**
 * Piętro z etykiet planu PWr w budynku szczegółowym, bez wpisywania liczby. Wspólny wybór piętra
 * w układzie chipów, jak w makiecie C2.
 *
 * Wymagania: US7.2, BR-G1
 */
@Composable
internal fun PlanFloorSection(floors: PlanFloors, floor: ReportFloor?, onEdit: (ReportEdit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_pietro))
        FloorPicker(
            options = FloorOptions(floors.items.map { FloorOption(key = it.id, label = it.label) }),
            selectedKey = (floor as? ReportFloor.Planned)?.floorId,
            onSelect = { option -> onEdit(ReportEdit.Floor(ReportFloor.Planned(option.key))) },
            layout = FloorPickerLayout.CHIPS,
        )
        if (floors.source.isNotEmpty()) {
            Text(
                text = stringResource(R.string.formularz_zrodlo_pieter, floors.source),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
