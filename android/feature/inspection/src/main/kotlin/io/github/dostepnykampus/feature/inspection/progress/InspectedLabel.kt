/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.pathIcon

/** Znacznik testowy ikony stanu „obejrzane”. */
internal const val INSPECTED_ICON_TAG: String = "ikona-obejrzane"

private val InspectedIcon: ImageVector =
    pathIcon(
        "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14z " +
            "M17.99 9l-1.41-1.42-6.59 6.59-2.58-2.57-1.42 1.41 4 3.99z",
    )

/**
 * Stan „obejrzane” piętra albo budynku: ikona i słowo [text]. Ikona jest ozdobna, znaczenie niesie słowo.
 *
 * Wymagania: US11.1, US11.3, US15.1, NFR-21
 *
 * @param text słowo stanu, np. „Obejrzane”
 * @param style styl słowa; domyślnie styl tekstu otoczenia z typografii motywu
 */
@Composable
internal fun InspectedLabel(text: String, style: TextStyle = LocalTextStyle.current) {
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
        Icon(InspectedIcon, contentDescription = null, modifier = Modifier.testTag(INSPECTED_ICON_TAG))
        Text(text = text, style = style)
    }
}
