/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.bar.PinnedButton
import io.github.dostepnykampus.core.designsystem.bar.PinnedButtonKind
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.BuildingLink
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader
import io.github.dostepnykampus.feature.map.overlay.SheetExits
import java.util.Locale

/**
 * Arkusz znaku planu w pomieszczeniu [id] z [plan] po dotknięciu znaku na mapie V1: nazwa znaku z „Zamknij”,
 * a pod nią wiersz budynku i piętro. Arkusz windy ma pod piętrem piętra windy z bieżącym oznaczonym „(tutaj jesteś)”,
 * a przy dużej czcionce zajmuje całą wysokość. TODO(#3052): klatki przy windzie. Na
 * końcu obrysowany przycisk zgłoszenia przy tym obiekcie; TalkBack czyta go z budynkiem i piętrem. Bez arkusza, gdy
 * [signOf] nie zwraca znaku. Systemowe „Wstecz” wywołuje [SheetExits.onBack].
 *
 * Wymagania: US16.1, US18.2, US2.8, NFR-18, NFR-20, NFR-53, US4.6, US14.2
 *
 * @param id identyfikator pomieszczenia dotkniętego znaku; `null` bez znaku
 * @param plan plan piętra na mapie; `null`, gdy planu nie widać
 * @param exits „Zamknij”, „Wstecz” i wiersz budynku
 * @param modifier położenie arkusza na mapie
 * @param onReport przycisk zgłoszenia przy obiekcie
 */
@Composable
internal fun ChosenPlanSheet(
    id: String?,
    plan: ShownPlan?,
    exits: SheetExits,
    modifier: Modifier = Modifier,
    onReport: (PlanReport) -> Unit = {},
) {
    val sign = id?.let { plan?.signOf(it) }
    BackHandler(enabled = sign != null, onBack = exits.onBack)
    val isLift = sign?.liftFloors.orEmpty().isNotEmpty()
    val isFull = isLift && LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    if (sign != null) {
        MapSheet(if (isFull) modifier.fillMaxHeight() else modifier) {
            MapSheetHeader(title = stringResource(sign.title), onClose = exits.onClose)
            BuildingLink(sign.building, onClick = { exits.onBuilding(sign.building) })
            Text(text = sign.floor, style = MaterialTheme.typography.bodyLarge)
            if (isLift) LiftStops(sign)
            sign.report?.let { ReportButton(it, onReport) }
        }
    }
}

@Composable
private fun LiftStops(sign: PlanSign) {
    val here = stringResource(R.string.mapa_winda_tutaj, sign.floor)
    val stops = sign.liftFloors.joinToString(separator = ", ") { if (it == sign.floor) here else it }
    Text(text = stringResource(R.string.mapa_winda_jedzie, stops), style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ReportButton(report: PlanReport, onReport: (PlanReport) -> Unit) {
    val text = PLAN_REPORT_BUTTONS[report.category]?.let { stringResource(it) } ?: return
    val floor = report.floorLabel.lowercaseFirst()
    val spoken = stringResource(R.string.mapa_zglos_obiekt_czytaj, text, report.building, floor)
    PinnedButton(
        text = text,
        onClick = { onReport(report) },
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = spoken },
        kind = PinnedButtonKind.Outlined,
    )
}

private val PLAN_REPORT_BUTTONS =
    mapOf(
        RoomCategory.ELEVATOR to R.string.mapa_zglos_winda,
        RoomCategory.ACCESSIBLE_TOILET to R.string.mapa_zglos_toaleta,
        RoomCategory.STAIRCASE to R.string.mapa_zglos_schody,
    )

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase(Locale.forLanguageTag("pl")) }
