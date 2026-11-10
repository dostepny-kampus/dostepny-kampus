/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia

/** Liczba kroków V4 przed wysłaniem (US4.1). */
internal const val QUICK_STEPS = 3

/** Znacznik testowy odcinka paska wykonanego kroku. */
internal const val STEP_DONE_TAG = "krok-wykonany"

/** Znacznik testowy odcinka paska kroku jeszcze przed osobą. */
internal const val STEP_AHEAD_TAG = "krok-przed"

/**
 * „Krok N z 3” i pod nim pasek z 3 odcinków; wykonane są wypełnione, a pozostałe mają tylko obrys, więc stan nie
 * zależy od samego koloru (US4.1, NFR-21). Napis i pasek są jednym elementem dla TalkBack: czyta „Krok N z 3”
 * z postępem (NFR-18).
 *
 * @param step bieżący krok od 1
 * @param modifier wymiary nadane przez ekran
 */
@Composable
internal fun QuickStepProgress(step: Int, modifier: Modifier = Modifier) {
    val progress = ProgressBarRangeInfo(current = step.toFloat(), range = 0f..QUICK_STEPS.toFloat(), steps = 2)
    Column(
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) { progressBarRangeInfo = progress },
        verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
    ) {
        Text(text = stringResource(R.string.szybkie_krok, step), fontWeight = Typografia.wyroznienieWaga)
        Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
            repeat(QUICK_STEPS) { index ->
                val isDone = index < step
                val shape = MaterialTheme.shapes.extraSmall
                val segment = Modifier.weight(1f).height(Odstepy.s)
                Box(
                    modifier =
                        if (isDone) {
                            segment.background(MaterialTheme.colorScheme.primary, shape).testTag(STEP_DONE_TAG)
                        } else {
                            segment
                                .border(Obramowania.ramka, MaterialTheme.colorScheme.outline, shape)
                                .testTag(STEP_AHEAD_TAG)
                        },
                )
            }
        }
    }
}
