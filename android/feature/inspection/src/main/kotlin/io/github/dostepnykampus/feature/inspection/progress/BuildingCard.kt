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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.feature.inspection.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val PERCENT = 100

private val READ_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.ROOT)

private val OpenIcon: ImageVector = pathIcon("M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z")

/**
 * Chwila odczytu postępu i „Odśwież”; postęp nie odświeża się sam.
 *
 * Wymagania: US11.2
 *
 * @param readAt chwila odczytu
 * @param isRefreshing czy trwa odświeżanie; wyłącza przycisk
 * @param onRefresh „Odśwież”
 */
@Composable
internal fun ReadLine(readAt: Instant, isRefreshing: Boolean, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(stringResource(R.string.postep_stan_z, READ_FORMAT.format(readAt.atZone(ZoneId.systemDefault()))))
        OutlinedButton(onClick = onRefresh, enabled = !isRefreshing) { Text(stringResource(R.string.postep_odswiez)) }
    }
}

/**
 * Karta budynku K1: symbol, postęp słowem, liczby zgłoszeń, wejścia sprawdzone na miejscu i „Otwórz”.
 * Budynek szczegółowy pokazuje piętra z procentem, podstawowy „Obejrzany” albo „Nieobejrzany”.
 *
 * Wymagania: US11.1, US11.3, US11.4, BR-21, NFR-21
 * TODO(#2340): nazwa budynku z katalogu pod symbolem.
 *
 * @param building postęp budynku
 * @param onOpen „Otwórz”: C5 budynku
 */
@Composable
internal fun BuildingCard(building: BuildingProgress, onOpen: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Odstepy.m), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(Odstepy.xs),
            ) {
                Text(building.buildingSymbol, style = MaterialTheme.typography.titleMedium)
                Text(coverageText(building.coverage))
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
