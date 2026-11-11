/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.markingImage
import io.github.dostepnykampus.core.model.EntranceMarking

private val SampleWidth = 34.dp
private val SampleHeight = 26.dp
private val SampleInset = 4.dp
private val SampleStroke = 2.5.dp

private val ENTRANCE_WORDS =
    listOf(
        EntranceMarking.ACCESSIBLE to R.string.karta_wiersz_dostepne,
        EntranceMarking.INACCESSIBLE to R.string.mapa_legenda_wejscie_niedostepne,
        EntranceMarking.NO_DATA to R.string.mapa_legenda_wejscie_bez_danych,
    )

/**
 * „Legenda” mapy V1 według makiety hi-fi: przycisk w lewym górnym rogu mapy, a pod nim, gdy [isOpen], panel ze
 * znakami mapy (US1.4). Przycisk ma cel dotyku 48 dp i słowo „Legenda”, które czyta TalkBack (NFR-17, NFR-18).
 *
 * @param isOpen czy panel legendy jest rozwinięty
 * @param onOpenChange dotknięcie przycisku albo „Zamknij” w panelu
 * @param modifier położenie na mapie
 */
@Composable
internal fun MapLegend(isOpen: Boolean, onOpenChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(Odstepy.s), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        val scheme = MaterialTheme.colorScheme
        val toggle = Modifier.clickable(role = Role.Button) { onOpenChange(!isOpen) }
        Surface(
            modifier = Modifier.heightIn(min = MapControlTouchSize).then(toggle),
            shape = CircleShape,
            color = if (isOpen) scheme.primaryContainer else scheme.surfaceContainerLowest,
            shadowElevation = Wysokosci.kontrolkiMapy,
            border = mapControlBorder(),
        ) {
            Row(
                modifier = Modifier.heightIn(min = MapControlTouchSize).padding(start = Odstepy.m, end = Odstepy.l),
                horizontalArrangement = Arrangement.spacedBy(Odstepy.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = MapControlIcons.Legend, contentDescription = null)
                Text(
                    text = stringResource(R.string.mapa_legenda),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = Typografia.wyroznienieWaga,
                )
            }
        }
        val panel = Modifier.weight(weight = 1f, fill = false)
        if (isOpen) LegendPanel(onClose = { onOpenChange(false) }, modifier = panel)
    }
}

@Composable
private fun LegendPanel(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Zaokraglenia.duze),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = Wysokosci.kontrolkiMapy,
        border = mapControlBorder(),
    ) {
        Column {
            LegendHeader(onClose)
            HorizontalDivider()
            LegendSigns(Modifier.weight(weight = 1f, fill = false).verticalScroll(scroll).padding(Odstepy.m))
            if (scroll.canScrollForward) ScrollHint()
        }
    }
}

@Composable
private fun LegendHeader(onClose: () -> Unit) {
    Row(Modifier.padding(start = Odstepy.m, end = Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.mapa_legenda),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.mapa_zamknij))
        }
    }
}

@Composable
private fun LegendSigns(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        LegendRow(stringResource(R.string.mapa_legenda_budynek)) { BuildingSample() }
        ENTRANCE_WORDS.forEach { (marking, word) ->
            LegendRow(stringResource(word)) { EntranceSample(marking) }
        }
        HorizontalDivider()
        Text(
            text = stringResource(R.string.mapa_legenda_zrodla),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScrollHint() {
    Column {
        HorizontalDivider()
        Text(
            text = stringResource(R.string.mapa_legenda_przewin),
            modifier = Modifier.padding(horizontal = Odstepy.m, vertical = Odstepy.s),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun LegendRow(word: String, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.m), verticalAlignment = Alignment.CenterVertically) {
        content()
        Text(text = word, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BuildingSample() {
    val colors = DostepnyKampusTheme.mapColors
    Canvas(Modifier.size(width = SampleWidth, height = SampleHeight)) {
        val inset = SampleInset.toPx()
        val inner = Size(width = size.width - 2 * inset, height = size.height - 2 * inset)
        drawRect(color = colors.pwr, topLeft = Offset(inset, inset), size = inner)
        val outline = Stroke(SampleStroke.toPx())
        drawRect(color = colors.pwrKontur, topLeft = Offset(inset, inset), size = inner, style = outline)
    }
}

@Composable
private fun EntranceSample(marking: EntranceMarking) {
    val colors = DostepnyKampusTheme.mapColors
    val sizePx = with(LocalDensity.current) { ENTRANCE_MARK_DP.dp.roundToPx() }
    val draw = { markingImage(marking = marking, sizePx = sizePx, shapeColor = colors.tusz, markColor = colors.papier) }
    val image = remember(marking, colors, sizePx) { draw() }
    Image(bitmap = image.asImageBitmap(), contentDescription = null, modifier = Modifier.width(SampleWidth))
}
