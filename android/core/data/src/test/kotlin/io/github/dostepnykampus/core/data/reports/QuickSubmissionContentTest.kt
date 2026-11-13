/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.reports

import io.github.dostepnykampus.core.model.BarrierGroup
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class QuickSubmissionContentTest {
    private val position =
        QuickReportLocation(
            point = GeoPoint(longitude = 17.06209, latitude = 51.10785),
            accuracyMeters = 6.0,
            isInBuilding = true,
        )

    @ParameterizedTest
    @EnumSource(BarrierGroup::class)
    fun `grupa i miejsce zachowane`(group: BarrierGroup) {
        val content =
            quickSubmissionContent(
                group = group,
                photoIds = listOf("zdjecie"),
                position = position,
                floorNumber = 2,
                entranceId = "A-1-w1",
            )
        assertEquals(
            SubmissionContent(
                kind = SubmissionKind.QUICK,
                location = position.point,
                photoIds = listOf("zdjecie"),
                accuracyMeters = 6.0,
                floor = SubmissionFloor(number = 2),
                barrierGroup = group,
                entranceId = "A-1-w1",
            ),
            content,
        )
    }

    @Test
    fun `piętro z planu ma pierwszeństwo`() {
        val content =
            quickSubmissionContent(
                group = BarrierGroup.TOILETS,
                photoIds = listOf("zdjecie"),
                position = position,
                floorNumber = 2,
                floorId = "A-1/p-1",
            )
        assertEquals(SubmissionFloor(floorId = "A-1/p-1"), content.floor)
    }

    @Test
    fun `poza konturem bez piętra`() {
        val content =
            quickSubmissionContent(
                group = BarrierGroup.TOILETS,
                photoIds = listOf("zdjecie"),
                position = position.copy(isInBuilding = false),
                floorNumber = 2,
                floorId = "A-1/p-1",
            )
        assertNull(content.floor)
        assertNull(content.buildingSymbol)
    }

    @Test
    fun `bez wyboru piętra`() {
        val content = quickSubmissionContent(BarrierGroup.TOILETS, listOf("zdjecie"), position)
        assertNull(content.floor)
    }
}
