/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.MapColors
import io.github.dostepnykampus.core.designsystem.MapaKontur
import io.github.dostepnykampus.core.designsystem.MapaPlan
import io.github.dostepnykampus.core.designsystem.MapaSciezka
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.marking.mapPlanSignImage
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.style.PLAN_CHOSEN_OUTLINE_WIDTH
import io.github.dostepnykampus.feature.map.style.barrierSign
import io.github.dostepnykampus.feature.map.style.entranceSign
import kotlin.math.ceil

private val SampleWidth = 34.dp
private val SampleHeight = 26.dp
private val SampleInset = 4.dp
private val PlanSignSampleDp = 24.dp
private val FloorlessRadius = 10.dp
private val FloorlessStroke = 1.5.dp
private const val SIGN_GRID = 24f
private const val QUESTION_STROKE = 2.2f
private const val QUESTION_MARK = "M9.6 9.6a2.5 2.5 0 1 1 3.4 2.3c-.6.3-1 .8-1 1.5v.4M12 16.6v.01"

private val ENTRANCE_WORDS =
    listOf(
        EntranceMarking.ACCESSIBLE to R.string.karta_wiersz_dostepne,
        EntranceMarking.INACCESSIBLE to R.string.mapa_legenda_wejscie_niedostepne,
        EntranceMarking.NO_DATA to R.string.mapa_legenda_wejscie_bez_danych,
    )

/** Wiersz legendy budynku jednego poziomu: słowo [word], tło [fill] i grubość konturu [stroke] jak na mapie. */
private data class BuildingLevel(val word: String, val fill: Color, val stroke: Dp)

/**
 * Znaki mapy V1, każdy z napisem, do arkusza legendy: budynki obu poziomów, ścieżka dostosowana, znaki wejść
 * i barier, a gdy [hasPlan], część „Plan piętra” ze znakami planu i zgłoszeniem bez piętra.
 *
 * Wymagania: US1.4, NFR-21, US1.3, US16.1, US18.1
 */
@Composable
internal fun LegendSigns(hasPlan: Boolean = false) {
    val mapColors = DostepnyKampusTheme.mapColors
    val buildings =
        listOf(
            BuildingLevel(
                stringResource(R.string.mapa_legenda_budynek_szczegolowy),
                mapColors.pwr,
                MapaKontur.szczegolowyDp.dp,
            ),
            BuildingLevel(
                stringResource(R.string.mapa_legenda_budynek_podstawowy),
                mapColors.pwrPodstawowy,
                MapaKontur.podstawowyDp.dp,
            ),
        )
    buildings.forEach { level ->
        LegendRow(level.word) {
            RectSample(fill = level.fill, outlineColor = mapColors.pwrKontur, stroke = level.stroke)
        }
    }
    LegendRow(stringResource(R.string.mapa_legenda_sciezka)) { PathBandAtLastZoomStop() }
    ENTRANCE_WORDS.forEach { (marking, word) ->
        LegendRow(stringResource(word)) {
            SignSample(marking) { colors, density -> entranceSign(marking, colors, density) }
        }
    }
    Severity.entries.forEach { severity ->
        LegendRow(stringResource(severity.nameRes())) {
            SignSample(severity) { colors, density -> barrierSign(severity, colors, density) }
        }
    }
    if (hasPlan) PlanLegendSigns()
}

/** Napis „przewiń” pod treścią arkusza, która się nie mieści. Wymagania: NFR-20 */
@Composable
internal fun ScrollHint() {
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
private fun PlanLegendSigns() {
    val mapColors = DostepnyKampusTheme.mapColors
    val wallStroke = MapaPlan.scianyDp.last().dp.dp
    HorizontalDivider()
    Text(
        text = stringResource(R.string.mapa_legenda_plan),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
    )
    listOf(
        R.string.mapa_legenda_plan_sala to mapColors.pSala,
        R.string.mapa_legenda_plan_korytarz to mapColors.pKorytarz,
    ).forEach { (word, fill) ->
        LegendRow(stringResource(word)) {
            RectSample(fill = fill, outlineColor = mapColors.pKontur, stroke = wallStroke)
        }
    }
    listOf(
        Triple(R.string.mapa_legenda_plan_klatka, mapColors.pInne, RoomCategory.STAIRCASE),
        Triple(R.string.karta_winda, mapColors.pWinda, RoomCategory.ELEVATOR),
        Triple(R.string.karta_toaleta, mapColors.pWc, RoomCategory.ACCESSIBLE_TOILET),
    ).forEach { (word, fill, category) ->
        LegendRow(stringResource(word)) {
            PlanRoomSignSample(fill = fill, wallStroke = wallStroke, category = category)
        }
    }
    LegendRow(stringResource(R.string.mapa_legenda_plan_szukana)) {
        RectSample(
            fill = mapColors.pWybrana,
            outlineColor = mapColors.pWybranaKontur,
            stroke = PLAN_CHOSEN_OUTLINE_WIDTH.dp,
        )
    }
    LegendRow(stringResource(R.string.mapa_legenda_plan_bez_pietra)) { FloorlessReportSample() }
}

@Composable
private fun LegendRow(word: String, content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.m), verticalAlignment = Alignment.CenterVertically) {
        content()
        Text(text = word, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RectSample(fill: Color, outlineColor: Color, stroke: Dp) {
    Canvas(Modifier.size(width = SampleWidth, height = SampleHeight)) {
        val inset = SampleInset.toPx()
        val inner = Size(width = size.width - 2 * inset, height = size.height - 2 * inset)
        drawRect(color = fill, topLeft = Offset(inset, inset), size = inner)
        drawRect(color = outlineColor, topLeft = Offset(inset, inset), size = inner, style = Stroke(stroke.toPx()))
    }
}

@Composable
private fun PlanRoomSignSample(fill: Color, wallStroke: Dp, category: RoomCategory) {
    val mapColors = DostepnyKampusTheme.mapColors
    Box(Modifier.size(width = SampleWidth, height = SampleHeight), contentAlignment = Alignment.Center) {
        RectSample(fill = fill, outlineColor = mapColors.pKontur, stroke = wallStroke)
        SignSample(category) { colors, density ->
            val sizePx = ceil(PlanSignSampleDp.value * density).toInt()
            mapPlanSignImage(category = category, sizePx = sizePx, colors = colors)
        }
    }
}

@Composable
private fun FloorlessReportSample() {
    val colors = DostepnyKampusTheme.mapColors
    val question = remember { PathParser().parsePathString(QUESTION_MARK).toPath() }
    Canvas(Modifier.size(width = SampleWidth, height = SampleHeight)) {
        val radius = FloorlessRadius.toPx()
        drawCircle(color = colors.papier, radius = radius)
        drawCircle(color = colors.tusz, radius = radius, style = Stroke(FloorlessStroke.toPx()))
        val diameter = 2 * radius
        translate(left = center.x - radius, top = center.y - radius) {
            scale(scale = diameter / SIGN_GRID, pivot = Offset.Zero) {
                drawPath(
                    path = question,
                    color = colors.tusz,
                    style = Stroke(width = QUESTION_STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Composable
private fun PathBandAtLastZoomStop() {
    val colors = DostepnyKampusTheme.mapColors
    val band = MapaSciezka.wstegaDp.last()
    val inside = MapaSciezka.wnetrzeDp.last()
    Canvas(Modifier.size(width = SampleWidth, height = SampleHeight)) {
        val bandWidth = band.dp.dp.toPx()
        val insideWidth = inside.dp.dp.toPx()
        val start = Offset(x = bandWidth / 2, y = center.y)
        val end = Offset(x = size.width - bandWidth / 2, y = center.y)
        drawLine(color = colors.sciezkaBrzeg, start = start, end = end, strokeWidth = bandWidth, cap = StrokeCap.Round)
        drawLine(
            color = colors.sciezkaWnetrze,
            start = start,
            end = end,
            strokeWidth = insideWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun SignSample(sign: Any, draw: (MapColors, Float) -> Bitmap) {
    val colors = DostepnyKampusTheme.mapColors
    val density = LocalDensity.current.density
    val image = remember(key1 = sign, key2 = colors, key3 = density) { draw(colors, density) }
    Image(bitmap = image.asImageBitmap(), contentDescription = null, modifier = Modifier.width(SampleWidth))
}
