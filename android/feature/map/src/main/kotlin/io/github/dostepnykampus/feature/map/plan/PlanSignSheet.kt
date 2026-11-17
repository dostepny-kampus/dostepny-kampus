/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader
import java.util.Locale

/**
 * Znak planu w arkuszu: nazwa [title], symbol budynku [building] i etykieta piętra [floor] z danych PWr.
 *
 * Wymagania: US16.1
 */
@Immutable
internal data class PlanSign(
    @param:StringRes val title: Int,
    val building: String,
    val floor: String,
)

/**
 * Znak toalety dostosowanej albo schodów w pomieszczeniu [id] tego planu; `null` dla pomieszczenia spoza planu
 * i dla innych grup. TODO(#2541): arkusz windy.
 *
 * Wymagania: US16.1
 */
internal fun ShownPlan.signOf(id: String): PlanSign? {
    val (floor, room) =
        rooms.firstNotNullOfOrNull { (floor, list) -> list.rooms.firstOrNull { it.id == id }?.let { floor to it } }
            ?: return null
    val title =
        when (room.category) {
            RoomCategory.ACCESSIBLE_TOILET -> R.string.mapa_plan_toaleta
            RoomCategory.STAIRCASE -> R.string.mapa_plan_schody
            else -> return null
        }
    val label = floors.firstOrNull { it.id == floor }?.label ?: return null
    return PlanSign(title = title, building = building, floor = label)
}

/**
 * Arkusz znaku planu w pomieszczeniu [id] z [plan] po dotknięciu znaku na mapie V1: nazwa znaku z „Zamknij”,
 * a pod nią budynek i piętro. Bez arkusza, gdy [signOf] nie zwraca znaku. Systemowe „Wstecz” zamyka arkusz
 * jak [onClose].
 *
 * Wymagania: US16.1, NFR-18, NFR-20, NFR-53
 *
 * @param id identyfikator pomieszczenia dotkniętego znaku; `null` bez znaku
 * @param plan plan piętra na mapie; `null`, gdy planu nie widać
 * @param onClose zamyka arkusz
 * @param modifier położenie arkusza na mapie
 */
@Composable
internal fun ChosenPlanSheet(id: String?, plan: ShownPlan?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val sign = id?.let { plan?.signOf(it) }
    BackHandler(enabled = sign != null, onBack = onClose)
    if (sign != null) {
        MapSheet(modifier) {
            MapSheetHeader(title = stringResource(sign.title), onClose = onClose)
            Text(
                text = stringResource(R.string.mapa_plan_miejsce, sign.building, sign.floor.lowercaseFirst()),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase(Locale.forLanguageTag("pl")) }
