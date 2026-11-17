/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.feature.map.R

/**
 * Nagłówek arkusza na mapie: znak [sign] przed tytułem [title] i obrysowany „Zamknij”, który wywołuje
 * [onClose]. Tytuł dostaje fokus po otwarciu arkusza. Przy dużej czcionce „Zamknij” stoi nad tytułem, żeby
 * tytuł miał całą szerokość arkusza.
 *
 * Wymagania: NFR-17, NFR-18, NFR-20, NFR-53
 *
 * @param title tytuł arkusza, nagłówek dla TalkBack
 * @param onClose zamyka arkusz
 * @param sign znak przed tytułem; `null` bez znaku
 */
@Composable
internal fun MapSheetHeader(title: String, onClose: () -> Unit, sign: (@Composable () -> Unit)? = null) {
    val header = remember { FocusRequester() }
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    LaunchedEffect(header) { withFrameMillis { header.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        if (isLarge) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { CloseButton(onClose) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
            sign?.invoke()
            Text(
                text = title,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(header)
                        .focusable()
                        .semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = Typografia.wyroznienieWaga,
            )
            if (!isLarge) CloseButton(onClose)
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
        Text(stringResource(R.string.mapa_zamknij))
    }
}
