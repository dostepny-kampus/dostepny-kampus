/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.feature.map.area.AreaEdgeAnnouncement
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.MapMotion
import io.github.dostepnykampus.feature.map.overlay.MapOverlay
import io.github.dostepnykampus.feature.map.overlay.MapStatusBar
import io.github.dostepnykampus.feature.map.overlay.dataDay
import io.github.dostepnykampus.feature.map.plan.FloorColumn

/** Sterowanie, kolumna pięter i arkusze nad widokiem mapy. Wymagania: US1.1, US2, US18.2 */
@Composable
internal fun CampusMapPanels(
    state: MapSceneState,
    catalog: BuildingCatalog?,
    areas: MapAreas,
    top: Dp,
    topInset: WindowInsets,
    sheet: ScreenSheet,
    isKeyboardOpen: Boolean,
    motion: MapMotion?,
    roomCover: Dp,
    plan: PlanLink,
    content: @Composable BoxScope.() -> Unit,
) {
    val camera = state.move?.camera ?: return
    val limits = state.limits ?: return
    Box(Modifier.fillMaxSize()) {
        FollowCamera(camera = camera, limits = limits, area = areas.current?.name, plan = plan)
        MapStatusBar(topInset, isShown = state.isCovered)
        val underBar = if (state.isCovered) topInset.asPaddingValues() else PaddingValues(top = top)
        MapOverlay(
            camera = camera,
            hasControls = state.hasControls && !isKeyboardOpen,
            dataDay = catalog?.dataDay(),
            onCameraChange = state::move,
            areas = areas,
            onAreaShow = state::land,
            modifier = Modifier.overlayArea(underBar = underBar, sheet = sheet),
            hasPanels = !state.isCovered && !isKeyboardOpen,
            hasNotice = !isKeyboardOpen,
            motion = motion,
            controlsCover = roomCover,
            start = { FloorColumn(plan = plan.shown, onChoose = plan.onFloor) },
            content = content,
        )
    }
}

private fun Modifier.overlayArea(underBar: PaddingValues, sheet: ScreenSheet): Modifier {
    val area = fillMaxSize().safeDrawingPadding().padding(underBar)
    val cover = sheet.cover ?: return area.padding(sheet.padding)
    return area.layout { measurable, constraints ->
        val bottom = cover().coerceIn(minimumValue = 0, maximumValue = constraints.maxHeight)
        val placeable = measurable.measure(constraints.offset(vertical = -bottom))
        layout(width = placeable.width, height = placeable.height + bottom) { placeable.place(x = 0, y = 0) }
    }
}

@Composable
private fun FollowCamera(camera: MapCamera, limits: MapLimits, area: String?, plan: PlanLink) {
    val report by rememberUpdatedState(plan.onView)
    val hide by rememberUpdatedState(plan.onHide)
    DisposableEffect(camera) {
        report(camera)
        onDispose {}
    }
    DisposableEffect(Unit) { onDispose { hide() } }
    AreaEdgeAnnouncement(camera = camera, limits = limits, area = area)
}
