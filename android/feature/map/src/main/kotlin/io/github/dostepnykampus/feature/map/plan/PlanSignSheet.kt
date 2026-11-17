/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.MapSheetHeader
import java.util.Locale

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
