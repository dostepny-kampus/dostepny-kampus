/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.location.GnssSystem
import io.github.dostepnykampus.core.location.Satellite
import kotlin.math.cos
import kotlin.math.sin

private const val ICON_VIEWPORT = 24f

private const val ZENITH_DEGREES = 90.0

private const val RING_STEP_DEGREES = 30

// Napisy na mapie nieba rosną z czcionką najwyżej 1,4 raza (wymagania ekranów, reguła 4; NFR-20).
private const val MAX_MAP_FONT_SCALE = 1.4f

private val MAP_SIZE = 240.dp

private val MARK_SIZE = 14.dp

private val RING_WIDTH = 1.dp

/**
 * Widoczne satelity do mapy nieba; opakowanie listy, żeby Compose wiedział, że się nie zmienia.
 *
 * @property satellites satelity z położeniem na niebie
 */
@Immutable
internal data class Sky(val satellites: List<Satellite>)

/** Kształt znaku systemu GNSS w skali 24 jednostek; system rozróżnia kształt i podpis, nie kolor (NFR-21). */
internal enum class SkyMark(data: String) {
    /** Koło: GPS. */
    CIRCLE("M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18z"),

    /** Kwadrat: Galileo. */
    SQUARE("M4 4h16v16H4z"),

    /** Trójkąt: GLONASS. */
    TRIANGLE("M12 2L22.5 20.5H1.5z"),

    /** Romb: BeiDou. */
    DIAMOND("M12 1.5L22.5 12L12 22.5L1.5 12z"),

    /** Krzyż: pozostałe systemy. */
    CROSS("M9 2h6v7h7v6h-7v7H9v-7H2V9h7z"),
    ;

    /** Ścieżka kształtu. */
    val path: Path by lazy { PathParser().parsePathString(data).toPath() }
}

/** Kształt znaku systemu na mapie nieba i w legendzie (US6.5). */
internal fun GnssSystem.mark(): SkyMark =
    when (this) {
        GnssSystem.GPS -> SkyMark.CIRCLE
        GnssSystem.GALILEO -> SkyMark.SQUARE
        GnssSystem.GLONASS -> SkyMark.TRIANGLE
        GnssSystem.BEIDOU -> SkyMark.DIAMOND
        GnssSystem.QZSS, GnssSystem.NAVIC, GnssSystem.SBAS, GnssSystem.UNKNOWN -> SkyMark.CROSS
    }

/**
 * Położenie satelity względem środka mapy o promieniu [radius]: zenit w środku, horyzont na brzegu, północ u góry,
 * wschód z prawej.
 */
internal fun skyOffset(azimuthDegrees: Double, elevationDegrees: Double, radius: Float): Offset {
    val elevation = elevationDegrees.coerceIn(minimumValue = 0.0, maximumValue = ZENITH_DEGREES)
    val distance = radius * (ZENITH_DEGREES - elevation) / ZENITH_DEGREES
    val angle = Math.toRadians(azimuthDegrees)
    return Offset(x = (distance * sin(angle)).toFloat(), y = (-distance * cos(angle)).toFloat())
}

/**
 * Mapa nieba C1 (US6.5): widoczne satelity na kołach wysokości co 30°, strony świata i legenda kształtów pod mapą.
 * Czytnik ekranu dostaje liczby satelitów według systemów (NFR-18).
 *
 * @param sky widoczne satelity
 * @param modifier wymiary nadane przez ekran
 */
@Composable
internal fun SkyMap(sky: Sky, modifier: Modifier = Modifier) {
    val resources = LocalResources.current
    val systems =
        sky.satellites
            .groupingBy(Satellite::system)
            .eachCount()
            .toSortedMap()
    val description =
        if (systems.isEmpty()) {
            stringResource(R.string.pomiar_mapa_nieba_pusta)
        } else {
            stringResource(R.string.pomiar_mapa_nieba_opis, resources.systemCounts(systems))
        }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        Text(text = stringResource(R.string.pomiar_mapa_nieba), style = MaterialTheme.typography.titleSmall)
        Sphere(sky = sky, description = description)
        systems.keys.groupBy(GnssSystem::mark).toSortedMap().forEach { (mark, marked) ->
            LegendRow(mark = mark, names = marked.joinToString(", ") { resources.systemName(it) })
        }
    }
}

@Composable
private fun Sphere(sky: Sky, description: String) {
    val density = LocalDensity.current
    val mapFontScale = density.fontScale.coerceAtMost(MAX_MAP_FONT_SCALE)
    val ring = MaterialTheme.colorScheme.outline
    val color = MaterialTheme.colorScheme.onSurface
    CompositionLocalProvider(
        LocalDensity provides Density(density = density.density, fontScale = mapFontScale),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.kierunek_polnoc))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.kierunek_zachod))
                Canvas(
                    modifier =
                        Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = MAP_SIZE)
                            .aspectRatio(1f)
                            .semantics { contentDescription = description },
                ) {
                    val markSize = MARK_SIZE.toPx()
                    val radius = size.minDimension / 2 - markSize / 2
                    for (elevation in 0 until ZENITH_DEGREES.toInt() step RING_STEP_DEGREES) {
                        val ringRadius = radius * (ZENITH_DEGREES.toFloat() - elevation) / ZENITH_DEGREES.toFloat()
                        drawCircle(color = ring, radius = ringRadius, style = Stroke(RING_WIDTH.toPx()))
                    }
                    sky.satellites.forEach { satellite ->
                        val at =
                            center +
                                skyOffset(
                                    azimuthDegrees = satellite.azimuthDegrees,
                                    elevationDegrees = satellite.elevationDegrees,
                                    radius = radius,
                                )
                        translate(left = at.x - markSize / 2, top = at.y - markSize / 2) {
                            drawMark(satellite.system.mark(), color, markSize)
                        }
                    }
                }
                Text(stringResource(R.string.kierunek_wschod))
            }
            Text(stringResource(R.string.kierunek_poludnie))
        }
    }
}

@Composable
private fun LegendRow(mark: SkyMark, names: String) {
    val color = MaterialTheme.colorScheme.onSurface
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(MARK_SIZE)) { drawMark(mark, color, size.minDimension) }
        Text(names)
    }
}

private fun DrawScope.drawMark(mark: SkyMark, color: Color, markSize: Float) {
    scale(scale = markSize / ICON_VIEWPORT, pivot = Offset.Zero) { drawPath(path = mark.path, color = color) }
}
