/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * Opcja wyboru piętra.
 *
 * @property key identyfikator opcji, np. id piętra z danych PWr albo numer piętra
 * @property label napis opcji, np. „Parter”, „1 piętro”, „Nie wiem”; w kolumnie czyta go TalkBack
 * @property short napis opcji w kolumnie pięter, np. „1” dla „1 piętro”
 */
public data class FloorOption(val key: String, val label: String, val short: String = label)

/**
 * Opcje wyboru piętra od góry listy albo od początku wiersza.
 *
 * @property items opcje w kolejności pokazania
 */
public data class FloorOptions(val items: List<FloorOption>)

/** Układ wyboru piętra (wymagania wspólne). */
public enum class FloorPickerLayout {
    /** Lista przycisków na całą szerokość: pytanie o piętro jest jedyną treścią kroku (V4). */
    LIST,

    /** Chipy w zawijanym wierszu: piętro jest jednym z pól formularza (C2). */
    CHIPS,

    /** Kolumna z napisem „Piętro” jak przyciski windy, wybrane w kółku: przełącznik planu na mapie (V1). */
    COLUMN,
}

/**
 * Wspólny wybór piętra na V4 i C2. TalkBack czyta grupę jako wybór jednej opcji, a każdą opcję
 * z rolą przycisku opcji i stanem „wybrane”. Wybraną opcję widać po obwódce [Obramowania.wybrane]
 * i znaku ✓, nie po samym kolorze. Cel dotyku ma co najmniej [CeleDotyku.minimum], a w liście
 * [CeleDotyku.przycisk]; napis rośnie z czcionką. Kolumna pokazuje krótkie napisy w kółkach, a TalkBack czyta
 * pełne; gdy się nie mieści, przewija się pod napisem „Piętro”, a wybrane piętro wraca w pole widzenia.
 *
 * Wymagania: US4.5, US7.2, NFR-18, NFR-21, NFR-17, NFR-20, US16.1
 *
 * @param options opcje do wyboru
 * @param selectedKey klucz wybranej opcji albo `null`, gdy nic nie wybrano
 * @param onSelect wybrana opcja
 * @param layout lista na całą szerokość albo chipy
 * @param modifier modyfikator zewnętrzny
 */
@Composable
public fun FloorPicker(
    options: FloorOptions,
    selectedKey: String?,
    onSelect: (FloorOption) -> Unit,
    layout: FloorPickerLayout,
    modifier: Modifier = Modifier,
) {
    val spacing = Arrangement.spacedBy(Odstepy.s)
    when (layout) {
        FloorPickerLayout.LIST -> {
            Column(modifier = modifier.selectableGroup(), verticalArrangement = spacing) {
                options.items.forEach { option ->
                    FloorChoice(
                        option = option,
                        isSelected = option.key == selectedKey,
                        onSelect = onSelect,
                        modifier = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk),
                    )
                }
            }
        }

        FloorPickerLayout.CHIPS -> {
            FlowRow(
                modifier = modifier.selectableGroup(),
                horizontalArrangement = spacing,
                verticalArrangement = spacing,
            ) {
                options.items.forEach { option ->
                    FloorChoice(
                        option = option,
                        isSelected = option.key == selectedKey,
                        onSelect = onSelect,
                        modifier = Modifier.heightIn(min = CeleDotyku.minimum),
                    )
                }
            }
        }

        FloorPickerLayout.COLUMN -> {
            FloorColumn(options = options, selectedKey = selectedKey, onSelect = onSelect, modifier = modifier)
        }
    }
}

@Composable
private fun FloorColumn(
    options: FloorOptions,
    selectedKey: String?,
    onSelect: (FloorOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainerLowest,
        contentColor = colors.onSurface,
        border = BorderStroke(Obramowania.ramka, colors.outline),
    ) {
        Column(
            modifier = Modifier.padding(Odstepy.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.pietro_kolumna),
                modifier = Modifier.padding(Odstepy.xs).semantics { heading() },
                style = MaterialTheme.typography.labelLarge,
            )
            Column(
                modifier =
                    Modifier
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .selectableGroup(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                options.items.forEach { option ->
                    FloorCircle(option = option, isSelected = option.key == selectedKey, onSelect = onSelect)
                }
            }
        }
    }
}

@Composable
private fun FloorCircle(option: FloorOption, isSelected: Boolean, onSelect: (FloorOption) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shown = remember { BringIntoViewRequester() }
    LaunchedEffect(isSelected) { if (isSelected) shown.bringIntoView() }
    val mark =
        if (isSelected) {
            Modifier
                .background(colors.primaryContainer, CircleShape)
                .border(Obramowania.wybrane, colors.onPrimaryContainer, CircleShape)
        } else {
            Modifier
        }
    Box(
        modifier =
            Modifier
                .bringIntoViewRequester(shown)
                .sizeIn(minWidth = CeleDotyku.minimum, minHeight = CeleDotyku.minimum)
                .clip(CircleShape)
                .then(mark)
                .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                .semantics { contentDescription = option.label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = option.short,
            modifier = Modifier.padding(Odstepy.xs).clearAndSetSemantics {},
            color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun FloorChoice(
    option: FloorOption,
    isSelected: Boolean,
    onSelect: (FloorOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val border =
        if (isSelected) {
            BorderStroke(Obramowania.wybrane, colors.onPrimaryContainer)
        } else {
            BorderStroke(Obramowania.pole, colors.outline)
        }
    Surface(
        modifier =
            modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) }),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) colors.primaryContainer else colors.surfaceContainerLowest,
        contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
        border = border,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isSelected) Text(text = CHECK, style = MaterialTheme.typography.labelLarge)
            Text(text = option.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

private const val CHECK = "✓"
