/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.dostepnykampus.core.designsystem.CeleDotyku
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
        if (floors.source.isNotEmpty()) {
            Text(
                text = stringResource(R.string.formularz_zrodlo_pieter, floors.source),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * „Typ bariery” (US7.1): pole z wybranym typem otwiera panel z listą 29 typów w 6 grupach. Panel ma u góry
 * nagłówek i „Zamknij”, które nie przewijają się z listą (NFR-18, NFR-53). Po otwarciu fokus jest na nagłówku,
 * po zamknięciu wraca na pole.
 *
 * @param type wybrany typ albo `null`
 * @param onEdit wybór typu
 */
@Composable
internal fun TypeSection(type: BarrierType?, onEdit: (ReportEdit) -> Unit) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var isReturning by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        SectionTitle(stringResource(R.string.formularz_typ))
        OutlinedButton(
            onClick = { isOpen = true },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CeleDotyku.przycisk)
                    .focusRequester(field)
                    .focusable(),
        ) {
            Text(
                text = type?.let { stringResource(it.nameRes()) } ?: stringResource(R.string.formularz_typ_wybierz),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    val close = {
        isOpen = false
        isReturning = true
    }
    if (isOpen) {
        TypePanel(
            onPick = { option ->
                onEdit(ReportEdit.Type(option))
                close()
            },
            onClose = close,
        )
    }
    LaunchedEffect(isReturning) {
        if (isReturning) {
            withFrameNanos {}
            field.requestFocus()
            isReturning = false
        }
    }
}

@Composable
private fun TypePanel(onPick: (BarrierType) -> Unit, onClose: () -> Unit) {
    val title = remember { FocusRequester() }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                TypePanelHeader(title = title, onClose = onClose)
                TypeList(onPick = onPick, modifier = Modifier.weight(1f))
            }
        }
        LaunchedEffect(title) {
            withFrameNanos {}
            title.requestFocus()
        }
    }
}

@Composable
private fun TypePanelHeader(title: FocusRequester, onClose: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(Odstepy.m),
        verticalArrangement = Arrangement.spacedBy(Odstepy.s),
    ) {
        Text(
            text = stringResource(R.string.formularz_typ),
            modifier = Modifier.focusRequester(title).focusable().semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.przycisk)) {
            Text(stringResource(R.string.formularz_zamknij))
        }
    }
}

@Composable
private fun TypeList(onPick: (BarrierType) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(bottom = Odstepy.m)) {
        BarrierGroup.entries.forEach { group ->
            Text(
                text = stringResource(group.nameRes()),
                modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s).semantics { heading() },
                style = MaterialTheme.typography.titleSmall,
            )
            BarrierType.entries.filter { it.group == group }.forEach { option ->
                TextButton(
                    onClick = { onPick(option) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk),
                ) {
                    Text(text = stringResource(option.nameRes()), modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
