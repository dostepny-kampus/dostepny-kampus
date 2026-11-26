/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.overlay.SheetExits

private val MapGround = Color(0xFFBEE3F2)

/**
 * Arkusz wejścia [shown] w motywie aplikacji, przy dolnej krawędzi tła w miejscu mapy.
 *
 * Wymagania: US2.1, US3.3
 *
 * @param shown wejście w arkuszu
 * @param exits „Zamknij”, „Wstecz” i wiersz budynku arkusza
 * @param actions czynności przy wejściu
 */
@Composable
internal fun EntranceSheetOnMap(
    shown: ShownEntrance,
    exits: SheetExits = SheetExits(onClose = {}),
    actions: EntranceActions = EntranceActions(),
) {
    DostepnyKampusTheme {
        Box(Modifier.fillMaxSize().background(MapGround)) {
            EntranceSheet(
                shown = shown,
                exits = exits,
                actions = actions,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
