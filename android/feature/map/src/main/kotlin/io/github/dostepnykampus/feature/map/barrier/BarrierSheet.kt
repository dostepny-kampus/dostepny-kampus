/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.campusDay
import io.github.dostepnykampus.core.designsystem.marking.SeverityIcon
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.BuildingLink
import io.github.dostepnykampus.feature.map.overlay.MapSheet
import io.github.dostepnykampus.feature.map.overlay.SheetExits

/**
 * Arkusz bariery na dole mapy V1 po dotknięciu znaku: plakietka uciążliwości ze znakiem i słowem,
 * obrysowany „Zamknij”, wiersz budynku, typ bariery tymi samymi napisami co na V2 jako nagłówek, miejsce
 * i „Sprawdzone 14.10.2026”. Wiersz budynku jest tylko dla bariery w budynku z katalogu: bariera poza
 * budynkiem nie ma budynku do pokazania (PW-5). Przy dużej czcionce arkusz się przewija.
 *
 * Miejsce składa [barrierPlace]. Dzień sprawdzenia liczy się w strefie czasu kampusu, nie telefonu.
 *
 * Wymagania: US1.5, US2.8, NFR-21, NFR-53, NFR-17, NFR-20
 *
 * @param chosen bariera z budynkiem i wejściem
 * @param exits „Zamknij” i wiersz budynku
 * @param modifier położenie arkusza na mapie
 */
@Composable
internal fun BarrierSheet(chosen: ChosenBarrier, exits: SheetExits, modifier: Modifier = Modifier) {
    MapSheet(modifier) {
        SheetTop(chosen.barrier.severity, exits.onClose)
        chosen.building?.let { building ->
            BuildingLink(building.symbol, onClick = { exits.onBuilding(building.symbol) })
        }
        BarrierFacts(chosen)
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

/**
 * Plakietka uciążliwości: znak i słowo.
 *
 * Wymagania: NFR-21
 */
@Composable
internal fun SeverityBadge(severity: Severity) {
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

/**
 * Typ bariery jako nagłówek krojem [titleStyle], miejsce z [barrierPlace], gdy [hasPlace], „Sprawdzone 14.10.2026”
 * i zdjęcie z [BarrierPhoto].
 *
 * Wymagania: US1.5, US2.2, BR-17, NFR-21
 */
@Composable
internal fun BarrierFacts(
    chosen: ChosenBarrier,
    hasPlace: Boolean = true,
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
) {
    val barrier = chosen.barrier
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(
            text = stringResource(barrier.type?.nameRes() ?: R.string.mapa_bariera_bez_typu),
            modifier = Modifier.semantics { heading() },
            style = titleStyle,
            fontWeight = Typografia.wyroznienieWaga,
        )
        if (hasPlace) BarrierPlace(chosen)
        barrier.verifiedAt?.let { verified ->
            val day = campusDay(verified)
            Text(
                text = stringResource(R.string.mapa_bariera_sprawdzone, day),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BarrierPhoto(chosen)
    }
}

@Composable
private fun BarrierPlace(chosen: ChosenBarrier) {
    Text(
        text =
            barrierPlace(
                symbol = chosen.barrier.buildingSymbol,
                name = chosen.building?.name,
                entrance = chosen.entrance,
                floor = chosen.barrier.floor,
            ),
        style = MaterialTheme.typography.bodyLarge,
    )
}
