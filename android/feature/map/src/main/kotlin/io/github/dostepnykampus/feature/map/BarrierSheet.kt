/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.PinnedButton
import io.github.dostepnykampus.core.designsystem.SeverityIcon
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.nameRes
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.Severity
import java.time.ZoneId

internal val CampusZone: ZoneId = ZoneId.of("Europe/Warsaw")

/**
 * Arkusz bariery na dole mapy V1 po dotknięciu znaku (US1.5): plakietka uciążliwości ze znakiem i słowem (NFR-21),
 * obrysowany „Zamknij” (NFR-53, NFR-17), typ bariery tymi samymi napisami co na V2 jako nagłówek, miejsce
 * i „Sprawdzone 14.10.2026”. „Pokaż opis budynku” jest tylko dla bariery w budynku z katalogu: bariera poza
 * budynkiem nie ma budynku do pokazania (PW-5). Przy dużej czcionce arkusz się przewija (NFR-20).
 *
 * Miejsce składa [barrierPlace]. Dzień sprawdzenia liczy się w strefie czasu kampusu, nie telefonu.
 *
 * @param chosen bariera z budynkiem i wejściem
 * @param onClose „Zamknij” zostawia samą mapę
 * @param onBuildingClick „Pokaż opis budynku” otwiera V2 budynku bariery
 * @param modifier położenie arkusza na mapie
 */
@Composable
internal fun BarrierSheet(
    chosen: ChosenBarrier,
    onClose: () -> Unit,
    onBuildingClick: (Building) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(Zaokraglenia.duze), shadowElevation = Odstepy.s) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Odstepy.m),
            verticalArrangement = Arrangement.spacedBy(Odstepy.s),
        ) {
            SheetTop(chosen.barrier.severity, onClose)
            BarrierFacts(chosen)
            chosen.building?.let { building ->
                PinnedButton(
                    text = stringResource(R.string.mapa_bariera_opis_budynku),
                    onClick = { onBuildingClick(building) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SheetTop(severity: Severity, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SeverityBadge(severity)
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.mapa_zamknij))
        }
    }
}

@Composable
private fun SeverityBadge(severity: Severity) {
    Surface(
        shape = RoundedCornerShape(Zaokraglenia.srednie),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeverityIcon(severity)
            Text(
                text = stringResource(severity.nameRes()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = Typografia.wyroznienieWaga,
            )
        }
    }
}

@Composable
private fun BarrierFacts(chosen: ChosenBarrier) {
    val barrier = chosen.barrier
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(barrier.type?.nameRes() ?: R.string.mapa_bariera_bez_typu),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = Typografia.wyroznienieWaga,
        )
        Text(text = barrierPlace(chosen), style = MaterialTheme.typography.bodyLarge)
        barrier.verifiedAt?.let { verified ->
            val day = DAY_FORMAT.format(verified.atZone(CampusZone).toLocalDate())
            Text(
                text = stringResource(R.string.mapa_bariera_sprawdzone, day),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
