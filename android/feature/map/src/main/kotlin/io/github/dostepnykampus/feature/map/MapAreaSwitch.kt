/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Wysokosci

/**
 * Przycisk terenu mapy V1 według makiety hi-fi: znak „⊙”, nazwa pokazanego terenu i znak rozwinięcia, a pod nim,
 * gdy [isOpen], panel „Teren na mapie” z listą terenów. Przycisk ma cel dotyku 48 dp i nazwę dla
 * TalkBack „Teren: Kampus główny. Zmień teren”. Po otwarciu panelu fokus stoi na nagłówku, a po
 * zamknięciu wraca na przycisk, dlatego przycisk przyjmuje fokus także w trybie dotyku; „Wstecz” zamyka
 * panel.
 *
 * Wymagania: US1.6, US1.7, NFR-17, NFR-18, NFR-20
 *
 * @param areas lista terenów; pusta nie pokazuje przycisku
 * @param isOpen czy panel jest otwarty
 * @param onOpenChange dotknięcie przycisku, „Zamknij” w panelu albo „Wstecz”
 * @param onChoose nazwa terenu wybranego na liście
 * @param modifier położenie na mapie
 */
@Composable
internal fun MapAreaSwitch(
    areas: AreaList,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shown = areas.choices.firstOrNull() ?: return
    val button = remember { FocusRequester() }
    var hasBeenOpen by remember { mutableStateOf(isOpen) }
    LaunchedEffect(isOpen) {
        if (hasBeenOpen && !isOpen) withFrameMillis { button.requestFocus() }
        hasBeenOpen = isOpen
    }
    BackHandler(enabled = isOpen) { onOpenChange(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        val scheme = MaterialTheme.colorScheme
        val label = stringResource(R.string.mapa_teren_przycisk, shown.name)
        val toggle =
            Modifier
                .focusRequester(button)
                .focusable()
                .clickable(role = Role.Button) { onOpenChange(!isOpen) }
                .semantics { contentDescription = label }
        Surface(
            modifier = Modifier.heightIn(min = MapControlTouchSize).then(toggle),
            shape = CircleShape,
            color = if (isOpen) scheme.primaryContainer else scheme.surfaceContainerLowest,
            shadowElevation = Wysokosci.kontrolkiMapy,
            border = mapControlBorder(),
        ) {
            Row(
                modifier =
                    Modifier
                        .heightIn(min = MapControlTouchSize)
                        .padding(start = Odstepy.m, end = Odstepy.s)
                        .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = MapControlIcons.Area, contentDescription = null)
                Text(
                    text = shown.name,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = Typografia.wyroznienieWaga,
                )
                Icon(if (isOpen) MapControlIcons.Up else MapControlIcons.Down, contentDescription = null)
            }
        }
        if (isOpen) {
            val panel = Modifier.weight(weight = 1f, fill = false)
            AreaPanel(areas = areas, onClose = { onOpenChange(false) }, onChoose = onChoose, modifier = panel)
        }
    }
}
