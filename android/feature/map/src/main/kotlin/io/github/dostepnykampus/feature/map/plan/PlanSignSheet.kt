/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader
import java.util.Locale

/**
 * Arkusz znaku planu w pomieszczeniu [id] z [plan] po dotknięciu znaku na mapie V1: nazwa znaku z „Zamknij”,
 * a pod nią budynek i piętro. Arkusz windy ma też piętra windy z bieżącym oznaczonym „(tutaj jesteś)”, a przy
 * dużej czcionce zajmuje całą wysokość. TODO(#3052): klatki przy windzie. Bez arkusza, gdy [signOf] nie zwraca
 * znaku. Systemowe „Wstecz” zamyka arkusz jak [onClose].
 *
 * Wymagania: US16.1, US18.2, NFR-18, NFR-20, NFR-53
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
    val isFull = sign?.liftFloors.orEmpty().isNotEmpty() && LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    if (sign != null) {
        MapSheet(if (isFull) modifier.fillMaxHeight() else modifier) {
            MapSheetHeader(title = stringResource(sign.title), onClose = onClose)
            Text(
                text = stringResource(R.string.karta_czytaj, sign.building, sign.floor.lowercaseFirst()),
                style = MaterialTheme.typography.bodyLarge,
            )
            if (sign.liftFloors.isNotEmpty()) {
                val here = stringResource(R.string.mapa_winda_tutaj, sign.floor)
                val stops = sign.liftFloors.joinToString(separator = ", ") { if (it == sign.floor) here else it }
                val text = stringResource(R.string.mapa_winda_jedzie, stops)
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun String.lowercaseFirst(): String = replaceFirstChar { it.lowercase(Locale.forLanguageTag("pl")) }
