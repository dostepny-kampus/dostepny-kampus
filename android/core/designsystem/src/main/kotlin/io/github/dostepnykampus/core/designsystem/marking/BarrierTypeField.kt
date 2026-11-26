/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import io.github.dostepnykampus.core.model.submission.BarrierType

/**
 * „Typ bariery”: nagłówek i pole z wybranym typem, które otwiera panel z listą typów w grupach. Panel ma u góry
 * nagłówek i „Zamknij”, które nie przewijają się z listą. Po otwarciu fokus jest na nagłówku panelu,
 * po zamknięciu wraca na pole.
 *
 * Wymagania: US7.1, US10.3, NFR-18, NFR-53
 *
 * @param type wybrany typ albo `null`
 * @param onPick wybór typu
 * @param modifier położenie pola wyznacza wołający
 * @param isError ramka pola w kolorze błędu, gdy typu brakuje przy czynności
 */
@Composable
public fun BarrierTypeField(
    type: BarrierType?,
    onPick: (BarrierType) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    var isReturning by remember { mutableStateOf(false) }
    val field = remember { FocusRequester() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        TypeTitle()
        OutlinedButton(
            onClick = { isOpen = true },
            border = fieldBorder(isError),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = CeleDotyku.przycisk)
                    .focusRequester(field)
                    .focusable(),
        ) {
            Text(
                text = type?.let { stringResource(it.nameRes()) } ?: stringResource(R.string.wybor_typ_wybierz),
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
                onPick(option)
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
private fun TypeTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.wybor_typ),
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.titleLarge,
    )
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
        TypeTitle(modifier = Modifier.focusRequester(title).focusable())
        OutlinedAction(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.przycisk)) {
            Text(stringResource(R.string.wybor_zamknij))
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
                style = MaterialTheme.typography.titleMedium,
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

@Composable
private fun fieldBorder(isError: Boolean): BorderStroke =
    if (isError) {
        BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.error)
    } else {
        ButtonDefaults.outlinedButtonBorder(enabled = true)
    }
