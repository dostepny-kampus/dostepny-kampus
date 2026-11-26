/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.card

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Typografia

/**
 * Wynik czynności pracownika w panelu informacyjnym ([InfoPanel]): tytuł [title] i opcjonalne objaśnienie
 * [message]. Panel jest regionem na żywo, więc zmiana treści jest ogłaszana.
 *
 * Wymagania: US12.1, US13.1, NFR-18
 */
@Composable
public fun StaffResultMessage(title: String, modifier: Modifier = Modifier, message: String? = null) {
    InfoPanel(modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(text = title, fontWeight = Typografia.wyroznienieWaga, style = MaterialTheme.typography.titleLarge)
        if (message != null) Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}
