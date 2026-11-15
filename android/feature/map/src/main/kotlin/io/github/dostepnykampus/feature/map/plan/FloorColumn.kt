/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.core.designsystem.FloorOption
import io.github.dostepnykampus.core.designsystem.FloorOptions
import io.github.dostepnykampus.core.designsystem.FloorPicker
import io.github.dostepnykampus.core.designsystem.FloorPickerLayout

/**
 * Piętra planu [plan] do kolumny pięter: najwyższe u góry, w kółku numer piętra (0 to parter), a TalkBack czyta
 * etykietę z danych, np. „Piwnica” albo „2 piętro”.
 *
 * Wymagania: US16.1, NFR-18
 */
internal fun floorOptions(plan: ShownPlan): FloorOptions =
    FloorOptions(plan.floors.reversed().map { FloorOption(key = it.id, label = it.label, short = "${it.order}") })

/**
 * Kolumna pięter planu [plan] przy prawej krawędzi mapy (R9); wybór piętra idzie do [onChoose] z symbolem budynku
 * i identyfikatorem piętra.
 *
 * Wymagania: US16.1, NFR-17, NFR-18, NFR-20
 */
@Composable
internal fun FloorColumn(
    plan: ShownPlan,
    onChoose: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FloorPicker(
        options = floorOptions(plan),
        selectedKey = plan.floor,
        onSelect = { onChoose(plan.building, it.key) },
        layout = FloorPickerLayout.COLUMN,
        modifier = modifier,
    )
}
