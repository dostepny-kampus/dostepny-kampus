/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Wysokosci
import io.github.dostepnykampus.core.designsystem.pathIcon
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.data.MapCamera
import io.github.dostepnykampus.feature.map.data.PanDirection
import io.github.dostepnykampus.feature.map.data.moved
import io.github.dostepnykampus.feature.map.data.zoomedIn
import io.github.dostepnykampus.feature.map.data.zoomedOut

private const val DARK_SURFACE_LUMINANCE = 0.5f

/** Cel dotyku pływającej kontrolki mapy. Wymagania: NFR-17 */
internal val MapControlTouchSize = 48.dp
internal val ControlCircleSize = 44.dp

/**
 * Obwódka pływającej kontrolki mapy: tylko w ciemnym motywie, gdzie cień słabo widać (tokeny wizualne,
 * `wysokosci.kontrolki_mapy`).
 */
@Composable
@ReadOnlyComposable
internal fun mapControlBorder(): BorderStroke? {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE
    return BorderStroke(Obramowania.ramka, Wysokosci.kontrolkiMapyObwodkaCiemny).takeIf { isDark }
}

/**
 * Przyciski mapy V1 według makiety hi-fi: białe koła z cieniem i szewronami w prawym górnym rogu. Strzałki
 * stoją w krzyżu, „Powiększ” i „Pomniejsz” w prawej kolumnie. Każdy przycisk zmienia [camera] bez gestu
 * i oddaje nowy widok przez [onCameraChange] (WCAG 2.5.1, WCAG 2.5.7). Jedna kontrolka to cel dotyku
 * 48 dp, a w nim koło z cieniem; jej nazwę czyta TalkBack, ikona jest tylko ozdobą.
 *
 * Wymagania: NFR-53, NFR-17, NFR-18
 */
@Composable
internal fun MapControls(camera: MapCamera, onCameraChange: (MapCamera) -> Unit, modifier: Modifier = Modifier) {
    val move = { direction: PanDirection -> onCameraChange(camera.moved(direction)) }
    Column(modifier.padding(Odstepy.s)) {
        Row {
            Spacer(Modifier.size(MapControlTouchSize))
            MapButton(R.string.mapa_przesun_w_gore, MapControlIcons.Up) { move(PanDirection.UP) }
            MapButton(R.string.mapa_powieksz, MapControlIcons.Plus) { onCameraChange(camera.zoomedIn()) }
        }
        Row {
            MapButton(R.string.mapa_przesun_w_lewo, MapControlIcons.Left) { move(PanDirection.LEFT) }
            Spacer(Modifier.size(MapControlTouchSize))
            MapButton(R.string.mapa_przesun_w_prawo, MapControlIcons.Right) { move(PanDirection.RIGHT) }
        }
        Row {
            Spacer(Modifier.size(MapControlTouchSize))
            MapButton(R.string.mapa_przesun_w_dol, MapControlIcons.Down) { move(PanDirection.DOWN) }
            MapButton(R.string.mapa_pomniejsz, MapControlIcons.Minus) { onCameraChange(camera.zoomedOut()) }
        }
    }
}

@Composable
internal fun MapButton(name: Int, icon: ImageVector, isActive: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val interactions = remember { MutableInteractionSource() }
    Box(
        modifier =
            Modifier.size(MapControlTouchSize).clickable(
                interactionSource = interactions,
                indication = ripple(bounded = false, radius = MapControlTouchSize / 2),
                onClickLabel = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(ControlCircleSize),
            shape = CircleShape,
            color = if (isActive) scheme.primaryContainer else scheme.surfaceContainerLowest,
            contentColor = scheme.onSurface,
            shadowElevation = Wysokosci.kontrolkiMapy,
            border = mapControlBorder(),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = stringResource(name))
            }
        }
    }
}

/**
 * Ikony mapy: szewrony, strzałka, plus, minus, znak wyboru, znak terenu i lista legendy z Material Icons,
 * siatka 24 × 24.
 */
internal object MapControlIcons {
    val Up: ImageVector = pathIcon("M12 8l-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z")
    val Down: ImageVector = pathIcon("M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z")
    val Left: ImageVector = pathIcon("M15.41 7.41L14 6l-6 6 6 6 1.41-1.41L10.83 12z")
    val Right: ImageVector = pathIcon("M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z")
    val Plus: ImageVector = pathIcon("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    val Minus: ImageVector = pathIcon("M19 13H5v-2h14v2z")
    val ArrowUp: ImageVector = pathIcon("M4 12l1.41 1.41L11 7.83V20h2V7.83l5.58 5.59L20 12l-8-8-8 8z")
    val Check: ImageVector = pathIcon("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")
    val Area: ImageVector =
        pathIcon(
            "M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10 10-4.49 10-10S17.51 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 " +
                "8 3.59 8 8-3.59 8-8 8zm3-8c0 1.66-1.34 3-3 3s-3-1.34-3-3 1.34-3 3-3 3 1.34 3 3z",
        )
    val Legend: ImageVector =
        pathIcon("M3 13h2v-2H3v2zm0 4h2v-2H3v2zm0-8h2V7H3v2zm4 4h14v-2H7v2zm0 4h14v-2H7v2zM7 7v2h14V7H7z")

    val Info: ImageVector =
        pathIcon(
            "M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18" +
                "c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z",
        )
}
