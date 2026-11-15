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
 * Słowa tekstu pięter z windą albo toaletą.
 *
 * @property onFloors „Na piętrach: %1$s.”
 * @property everywhere „Na każdym piętrze: %1$s – %2$s.”
 * @property missing „Nie ma na piętrach: %1$s.”
 * @property and spójnik przed ostatnią pozycją listy
 */
internal data class PresenceWords(val onFloors: String, val everywhere: String, val missing: String, val and: String)

/**
 * Tekst pięter z pomieszczeniem [presence] w słowach [words]: lista pięter z liczbą w nawiasie przy kilku i piętra
 * bez niego albo zakres od najniższego do najwyższego, gdy jest na każdym piętrze.
 *
 * Wymagania: US16.3, NFR-40
 */
internal fun presenceText(presence: FloorPresence, words: PresenceWords): String {
    val floors = presence.floors
    if (presence.missing.isEmpty()) {
        return words.everywhere.format(floors.first().name, floors.last().name)
    }
    val listed = floors.map { if (it.count > 1) "${it.name} (${it.count})" else it.name }
    val present = words.onFloors.format(joined(listed, words.and))
    return "$present ${words.missing.format(joined(presence.missing, words.and))}"
}

/** Pozycje [items] po przecinku, ostatnia po spójniku [and], np. „a, b i c”. */
internal fun joined(items: List<String>, and: String): String =
    if (items.size < 2) items.joinToString() else "${items.dropLast(1).joinToString()} $and ${items.last()}"

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
            facts.elevator?.let { FactRow(title = stringResource(R.string.karta_winda), text = presenceText(it, words)) }
            facts.toilet?.let {
                val text = "${stringResource(R.string.karta_toaleta_dla)} ${presenceText(it, words)}"
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
