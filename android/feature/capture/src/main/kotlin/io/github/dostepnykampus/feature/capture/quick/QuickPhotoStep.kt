/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionScope
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.navigation.ReportRoute
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import io.github.dostepnykampus.feature.capture.photo.PhotoTranscoder
import io.github.dostepnykampus.feature.capture.photo.rememberPhotoCamera
import io.github.dostepnykampus.feature.capture.report.form.PhotoSection
import io.github.dostepnykampus.feature.capture.report.form.takeLabel

/**
 * Miejsce na przypięte czynności kroku V4 pod przewijaną treścią. Krok wstawia je, gdy jest na ekranie, i zdejmuje,
 * gdy znika, np. gdy zamiast niego stoi prośba o zgodę na lokalizację.
 *
 * Wymagania: NFR-52
 */
@Stable
internal class QuickPinnedSlot {
    /** Przypięte czynności bieżącego kroku; `null`, gdy krok ich nie ma. */
    var content: (@Composable PinnedActionScope.() -> Unit)? by mutableStateOf(null)
}

/**
 * Krok 1 V4: komunikat „Ważne” i podgląd zdjęcia w treści, a „Zrób zdjęcie” jako jedyna wypełniona czynność
 * przypięta w [slot]. Po zdjęciu przypięte „Dalej” stoi nad „Zrób nowe zdjęcie”; przy dużej czcionce przypięte
 * zostaje samo „Dalej”, a „Zrób nowe zdjęcie” stoi na końcu treści.
 *
 * Wymagania: US4.1, US4.8, NFR-52, NFR-20
 *
 * @param route krok 1 ze zdjęciem po powrocie z kroku 2 albo bez niego
 * @param onNext przejście do kroku 2 ze zdjęciem
 * @param slot miejsce na przypięte czynności ekranu
 * @param transcoder przekodowanie zdjęcia, które przetrwa obrót ekranu
 */
@Composable
internal fun QuickPhotoStep(
    route: ReportRoute,
    onNext: (ReportRoute) -> Unit,
    slot: QuickPinnedSlot,
    transcoder: PhotoTranscoder = hiltViewModel(),
) {
    val camera =
        rememberPhotoCamera(
            onPhoto = { onNext(route.copy(photoId = it.photoId, photoPath = it.path)) },
            transcoder = transcoder,
        )
    val photos = PhotoPaths(listOfNotNull(route.photoPath))
    val hasPhoto = route.photoPath != null
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    val take = takeLabel(photos)
    PhotoSection(photos = photos, camera = camera, title = null, isTakeShown = false)
    if (hasPhoto && isLarge) {
        val wide = Modifier.fillMaxWidth().heightIn(min = CeleDotyku.przycisk)
        FilledTonalButton(onClick = camera.onTake, modifier = wide) { Text(take) }
    }
    val next = stringResource(R.string.pomiar_dalej)
    PinnedIn(slot) {
        if (hasPhoto) {
            PinnedButton(text = next, onClick = { onNext(route) }, modifier = Modifier.fillMaxWidth())
            if (!isLarge) {
                val wide = Modifier.fillMaxWidth()
                PinnedButton(text = take, onClick = camera.onTake, modifier = wide, kind = PinnedButtonKind.Tonal)
            }
        } else {
            PinnedButton(text = take, onClick = camera.onTake, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PinnedIn(slot: QuickPinnedSlot, content: @Composable PinnedActionScope.() -> Unit) {
    SideEffect { slot.content = content }
    DisposableEffect(slot) { onDispose { slot.content = null } }
}
