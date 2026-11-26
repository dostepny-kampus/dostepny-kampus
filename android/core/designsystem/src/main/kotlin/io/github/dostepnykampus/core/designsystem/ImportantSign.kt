/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

/** Znacznik testowy znaku „!” w kole. */
public const val IMPORTANT_SIGN_TAG: String = "znak-wazne"

private val ImportantIcon: ImageVector =
    pathIcon(
        path =
            "M11 15h2v2h-2zm0-8h2v6h-2zm0.99-5C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 " +
                "11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z",
        name = "Important",
    )

/**
 * Znak „!” w kole w rozmiarze [Ikony.rozmiar], bez opisu dostępności: znaczenie niesie tekst obok.
 *
 * Wymagania: NFR-21
 *
 * @param tint kolor znaku
 * @param modifier położenie nadane przez wiersz z tekstem
 */
@Composable
public fun ImportantSign(tint: Color, modifier: Modifier = Modifier) {
    Icon(
        imageVector = ImportantIcon,
        contentDescription = null,
        modifier = modifier.size(Ikony.rozmiar).testTag(IMPORTANT_SIGN_TAG),
        tint = tint,
    )
}
