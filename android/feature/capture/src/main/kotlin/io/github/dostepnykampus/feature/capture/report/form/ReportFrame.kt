/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.form

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.bar.BackHeader
import io.github.dostepnykampus.core.designsystem.bar.PinnedActionBar
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.AdaptiveScreen

/** Układ formularza z nagłówkiem [title], powrotem [onBack] i czynnością [actionText] wywołującą [onSubmit]. */
@Composable
internal fun ReportFrame(
    title: String,
    onBack: () -> Unit,
    actionText: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    AdaptiveScreen(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing),
        header = { BackHeader(title = title, onBack = onBack) },
        actions = { PinnedActionBar { PinnedButton(text = actionText, onClick = onSubmit) } },
        content = content,
    )
}
