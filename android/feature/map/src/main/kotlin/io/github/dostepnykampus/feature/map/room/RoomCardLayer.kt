/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.barrier.ShownBarriers
import io.github.dostepnykampus.feature.map.entrance.EntranceActions
import io.github.dostepnykampus.feature.map.entrance.EntranceSheet
import io.github.dostepnykampus.feature.map.entrance.ShownEntrance
import io.github.dostepnykampus.feature.map.overlay.SheetExits
import io.github.dostepnykampus.feature.map.plan.PlanReport
import io.github.dostepnykampus.feature.map.plan.PlanSign
import io.github.dostepnykampus.feature.map.plan.PlanSignSheet

/** Karta [room] albo otwarte z niej szczegóły kroku, z powrotem do tego kroku. Wymagania: US18.2 */
@Composable
internal fun RoomCardLayer(
    room: OpenRoom,
    onBuilding: (String) -> Unit,
    barriers: ShownBarriers,
    actions: EntranceActions,
    modifier: Modifier = Modifier,
    onShowMap: (() -> Unit)? = null,
) {
    val card = room.card
    var detail by rememberSaveable(card.found.building, card.found.room.id) { mutableStateOf<RoomStepDetail?>(null) }
    val exits = SheetExits(onClose = { detail = null }, onBuilding = onBuilding)
    val way = card.way as? RoomWay.Steps
    val shownDetail = detail
    if (way != null && shownDetail != null) {
        when (shownDetail) {
            RoomStepDetail.ENTRANCE -> {
                RoomEntranceDetails(
                    way = way,
                    barriers = barriers,
                    exits = exits,
                    actions = actions,
                    modifier = modifier,
                    isFull = onShowMap != null,
                )
            }

            RoomStepDetail.LIFT -> {
                way.liftSign(card.found.building)?.let { sign ->
                    PlanSignSheet(
                        sign = sign,
                        exits = exits,
                        modifier = modifier,
                        onReport = actions.onPlanReport,
                        isFullScreen = onShowMap != null,
                    )
                }
            }
        }
    } else {
        RoomCardSheet(
            room = room,
            onBuilding = onBuilding,
            modifier = modifier,
            onShowMap = onShowMap,
            onDetails = { detail = it },
        )
    }
}

@Composable
private fun RoomEntranceDetails(
    way: RoomWay.Steps,
    barriers: ShownBarriers,
    exits: SheetExits,
    actions: EntranceActions,
    modifier: Modifier,
    isFull: Boolean,
) {
    val entrance = way.entrance
    DisposableEffect(entrance) {
        actions.onShow(entrance)
        onDispose {}
    }
    BackHandler(onBack = exits.onBack)
    val shown =
        ShownEntrance(
            entrance = entrance,
            isRecommended = true,
            barriers = barriers.barriers.filter { it.entranceId == entrance.id },
            lift = actions.lift?.takeIf { it.entranceId == entrance.id },
        )
    EntranceSheet(shown = shown, exits = exits, actions = actions, modifier = modifier, isFull = isFull)
}

private fun RoomWay.Steps.liftSign(building: String): PlanSign? {
    val step = lift ?: return null
    val report =
        PlanReport(
            category = step.room.category,
            building = building,
            floorId = entranceFloor.id,
            floorLabel = entranceFloor.label,
            point = step.room.labelPoint,
        )
    return PlanSign(
        title = R.string.karta_winda,
        building = building,
        floor = entranceFloor.label,
        liftFloors = step.stops,
        report = report,
    )
}
