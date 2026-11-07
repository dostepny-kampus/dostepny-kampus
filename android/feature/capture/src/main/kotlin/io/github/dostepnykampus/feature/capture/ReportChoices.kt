/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.nameRes
import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.BarrierType

/**
 * Piętro wpisane liczbą w budynku bez planu PWr, z polem „Półpiętro” (US7.2, BR-G1, PW-6).
 * Wpisany tekst należy do budynku: po „Zmień miejsce” na inny budynek pole startuje od piętra ze stanu.
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

/** Piętro z etykiet planu PWr w budynku szczegółowym, bez wpisywania liczby (US7.2, BR-G1). */
@Composable
internal fun PlanFloorSection(floors: PlanFloors, floor: ReportFloor?, onEdit: (ReportEdit) -> Unit) {
    val chosen = (floor as? ReportFloor.Planned)?.floorId
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_pietro))
        floors.items.forEach { option ->
            Row(
                modifier =
                    Modifier.fillMaxWidth().selectable(selected = option.id == chosen, role = Role.RadioButton) {
                        onEdit(ReportEdit.Floor(ReportFloor.Planned(option.id)))
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = option.id == chosen, onClick = null)
                Text(text = option.label, modifier = Modifier.padding(start = Odstepy.s))
            }
        }
    }
}

/** „Typ bariery” pogrupowany w 6 grup barier (US7.1, BR-7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TypeSection(type: BarrierType?, onEdit: (ReportEdit) -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_typ))
        ExposedDropdownMenuBox(expanded = isOpen, onExpandedChange = { isOpen = it }) {
            OutlinedTextField(
                value = type?.let { stringResource(it.nameRes()) }.orEmpty(),
                onValueChange = {},
                modifier =
                    Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                readOnly = true,
                placeholder = { Text(stringResource(R.string.formularz_typ_wybierz)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isOpen) },
            )
            ExposedDropdownMenu(expanded = isOpen, onDismissRequest = { isOpen = false }) {
                BarrierGroup.entries.forEach { group ->
                    Text(
                        text = stringResource(group.nameRes()),
                        modifier = Modifier.padding(Odstepy.m).semantics { heading() },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    BarrierType.entries.filter { it.group == group }.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(stringResource(option.nameRes())) },
                            onClick = {
                                onEdit(ReportEdit.Type(option))
                                isOpen = false
                            },
                        )
                    }
                }
            }
        }
    }
}
