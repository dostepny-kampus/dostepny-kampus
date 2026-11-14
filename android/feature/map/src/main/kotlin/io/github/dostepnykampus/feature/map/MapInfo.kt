/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.model.AssessmentSource
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.feature.map.building.DAY_FORMAT
import java.time.LocalDate

/** Strona praw autorskich OpenStreetMap, cel odnośnika w podpisie mapy i w panelu „Źródła danych”. Wymagania: NFR-37 */
internal const val OSM_COPYRIGHT = "https://www.openstreetmap.org/copyright"

/** Dzień stanu danych z importu: najpóźniejszy dzień oznaczenia wejść ze źródła PWr; `null`, gdy takich nie ma. */
internal fun BuildingCatalog.dataDay(): LocalDate? {
    val imported = entrances.mapNotNull { it.assessment }.filter { it.source == AssessmentSource.IMPORT }
    return imported.maxOfOrNull { it.date }
}

/**
 * Przycisk „ⓘ” mapy V1 według makiety hi-fi: koło w prawym dolnym rogu mapy, a nad nim, gdy [isOpen], panel
 * „Źródła danych” z licencją i źródłami. Cel dotyku 48 dp, nazwę „Informacje o mapie” czyta
 * TalkBack.
 *
 * Wymagania: NFR-37, NFR-40, NFR-17, NFR-18
 *
 * @param isOpen czy panel jest otwarty
 * @param dataDay dzień stanu danych PWr albo `null`, gdy dane go nie podają
 * @param onOpenChange dotknięcie przycisku albo „Zamknij” w panelu
 * @param modifier położenie na mapie
 */
@Composable
internal fun MapInfo(
    isOpen: Boolean,
    dataDay: LocalDate?,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = Odstepy.s), horizontalAlignment = Alignment.End) {
        if (isOpen) {
            val panel = Modifier.weight(weight = 1f, fill = false).padding(bottom = Odstepy.s)
            InfoPanel(dataDay = dataDay, onClose = { onOpenChange(false) }, modifier = panel)
        }
        MapButton(name = R.string.mapa_informacje, icon = MapControlIcons.Info, isActive = isOpen) {
            onOpenChange(!isOpen)
        }
    }
}

@Composable
private fun InfoPanel(dataDay: LocalDate?, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = Wysokosci.kontrolkiMapy,
        border = mapControlBorder(),
    ) {
        val inner = Modifier.verticalScroll(rememberScrollState()).padding(Odstepy.m)
        Column(inner, verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
            InfoHeader(onClose)
            StreetSource()
            val day = dataDay?.let { DAY_FORMAT.format(it) }
            val campus =
                if (day == null) {
                    stringResource(R.string.mapa_zrodla_pwr)
                } else {
                    stringResource(R.string.mapa_zrodla_pwr_stan, day)
                }
            Text(text = campus, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun InfoHeader(onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.mapa_zrodla),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.mapa_zamknij))
        }
    }
}

@Composable
private fun StreetSource() {
    val link = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    val before = stringResource(R.string.mapa_zrodla_osm_przed)
    val name = stringResource(R.string.mapa_osm)
    val after = stringResource(R.string.mapa_zrodla_osm_po)
    val text =
        buildAnnotatedString {
            append(before)
            withLink(LinkAnnotation.Url(OSM_COPYRIGHT, TextLinkStyles(link))) { append(name) }
            append(after)
        }
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}
