/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.feature.inspection.R

private const val PERCENT = 100

private val OpenIcon: ImageVector = pathIcon("M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z")

/**
 * Karta budynku K1: symbol, pod nim nazwa z katalogu, postęp słowem, liczby zgłoszeń, wejścia sprawdzone na miejscu
 * i „Otwórz”. Budynek szczegółowy pokazuje piętra z procentem, podstawowy „Obejrzany” albo „Nieobejrzany”.
 *
 * Wymagania: US11.1, US11.3, US11.4, BR-21, NFR-21
 *
 * @param building postęp budynku
 * @param name nazwa budynku z katalogu; `null` zostawia sam symbol
 * @param onOpen „Otwórz”: C5 budynku
 */
@Composable
internal fun BuildingCard(building: BuildingProgress, name: String?, onOpen: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
            ) {
                Text(building.buildingSymbol, style = MaterialTheme.typography.titleMedium)
                name?.let { Text(it) }
                val coverage = building.coverage
                if (coverage is InspectionCoverage.Whole && coverage.isInspected) {
                    InspectedLabel(stringResource(R.string.postep_obejrzany))
                } else {
                    Text(coverageText(coverage))
                }
                Text(stringResource(R.string.postep_czeka, building.pendingSubmissions))
                Text(stringResource(R.string.postep_przyjete, building.acceptedSubmissions))
                if (building.coverage is InspectionCoverage.Floors) {
                    val checked = building.entrancesCheckedOnSite
                    Text(stringResource(R.string.postep_wejscia, checked, building.entrancesTotal))
                }
            }
            OutlinedButton(onClick = onOpen) {
                Text(stringResource(R.string.postep_otworz))
                Icon(imageVector = OpenIcon, contentDescription = null, modifier = Modifier.padding(start = Odstepy.s))
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun coverageText(coverage: InspectionCoverage): String =
    when (coverage) {
        is InspectionCoverage.Floors -> {
            val (inspected, total) = coverage.floorsInspected to coverage.floorsTotal
            val percent = if (total == 0) 0 else inspected * PERCENT / total
            stringResource(R.string.postep_pietra, inspected, total, percent)
        }

        is InspectionCoverage.Whole -> {
            stringResource(if (coverage.isInspected) R.string.postep_obejrzany else R.string.postep_nieobejrzany)
        }
    }
