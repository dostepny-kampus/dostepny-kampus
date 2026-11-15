/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.GroupContainer
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.R

/**
 * Grupa „Winda i toaleta” z faktów o piętrach [facts]; bez windy i toalety dostosowanej w planach grupy nie ma.
 *
 * Wymagania: US16.3, US2.3, NFR-40
 */
@Composable
internal fun LiftToiletGroup(facts: FloorFacts) {
    if (facts.elevator == null && facts.toilet == null) return
    val words =
        PresenceWords(
            onFloors = stringResource(R.string.karta_na_pietrach),
            everywhere = stringResource(R.string.karta_kazde_pietro),
            missing = stringResource(R.string.karta_nie_ma_na_pietrach),
            and = stringResource(R.string.karta_i),
        )
    GroupContainer(title = stringResource(R.string.karta_winda_toaleta)) {
        Column(verticalArrangement = Arrangement.spacedBy(Odstepy.m)) {
            facts.elevator?.let { lift ->
                FactRow(title = stringResource(R.string.karta_winda), text = presenceText(lift, words))
            }
            facts.toilet?.let { toilet ->
                val text = "${stringResource(R.string.karta_toaleta_dla)} ${presenceText(toilet, words)}"
                FactRow(title = stringResource(R.string.karta_toaleta), text = text)
            }
        }
    }
}

@Composable
private fun FactRow(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.xs)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
