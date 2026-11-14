/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/**
 * Opcja wyboru piętra.
 *
 * @property key identyfikator opcji, np. id piętra z danych PWr albo numer piętra
 * @property label napis opcji, np. „Parter”, „1 piętro”, „Nie wiem”
 */
public data class FloorOption(val key: String, val label: String)

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
}

/**
 * Wspólny wybór piętra na V4 i C2. TalkBack czyta grupę jako wybór jednej opcji, a każdą opcję
 * z rolą przycisku opcji i stanem „wybrane”. Wybraną opcję widać po obwódce [Obramowania.wybrane]
 * i znaku ✓, nie po samym kolorze. Cel dotyku ma co najmniej [CeleDotyku.minimum], a w liście
 * [CeleDotyku.przycisk]; napis rośnie z czcionką.
 *
 * Wymagania: US4.5, US7.2, NFR-18, NFR-21, NFR-17, NFR-20
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
