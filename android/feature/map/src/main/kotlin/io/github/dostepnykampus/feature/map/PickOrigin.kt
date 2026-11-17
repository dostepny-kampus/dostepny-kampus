/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.camera.CameraTransition
import io.github.dostepnykampus.feature.map.camera.FreeMap
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.cameraTransition
import io.github.dostepnykampus.feature.map.camera.placed
import io.github.dostepnykampus.feature.map.camera.tapTransition
import io.github.dostepnykampus.feature.map.camera.within
import io.github.dostepnykampus.feature.map.campus.CampusBounds

/**
 * Skąd pochodzi wybór budynku i jaki ruch mapy z niego wynika. Pierwszy wybór na mapie, która powstała już pod
 * kartą (wybór z listy budynków, [isUnderCard]), staje bez ruchu kamery ([placed]). Budynek wybrany dotknięciem
 * konturu rusza mapę według [tapTransition], pozostałe wybory (wyszukiwanie) według [cameraTransition]. Nie jest
 * stanem kompozycji.
 *
 * Wymagania: US2.8, NFR-19, NFR-56
 */
internal class PickOrigin(isUnderCard: Boolean) {
    private var isPlacing = isUnderCard
    private var tapped: String? = null
    private var selected: String? = null

    /** Budynek [symbol] wybrany dotknięciem konturu albo w arkuszu „Który budynek?”. */
    fun tap(symbol: String) {
        tapped = symbol
    }

    /**
     * Zaznaczony budynek [building]. Zamknięcie karty (zaznaczenie znika) zapomina dotknięcie, więc następny wybór
     * z wyszukiwania rusza mapę zwykle; dotknięcie przy zamkniętej karcie czeka na swój budynek.
     */
    fun follow(building: String?) {
        if (building == null && selected != null) tapped = null
        selected = building
    }

    /**
     * Ruch do budynku [building] w kadrze [view]. Wyszukiwanie przy otwartej karcie trzyma powiększenie
     * ([keepsZoom]); dotknięcie liczy się od widoku przesuniętego o [belowDp], zob. [tapTransition].
     */
    fun move(building: String?, view: PickView, keepsZoom: Boolean, belowDp: Double): CameraTransition {
        val move = if (building != null && building == tapped) view.tapped(belowDp) else view.searched(keepsZoom)
        val bounded = move.within(view.limits, free = view.free)
        val taken = if (isPlacing) bounded.placed(animationScale = view.scale) else bounded
        isPlacing = false
        return taken
    }

    private fun PickView.tapped(belowDp: Double) =
        tapTransition(from = from, outline = outline, free = free, belowDp = belowDp, animationScale = scale)

    private fun PickView.searched(keepsZoom: Boolean) =
        cameraTransition(from = from, outline = outline, free = free, animationScale = scale, keepsZoom = keepsZoom)
}

/**
 * Kadr ruchu do wybranego budynku.
 *
 * @property from widok, od którego zaczyna się ruch
 * @property outline kontur wybranego budynku
 * @property free wolna część mapy nad kartą
 * @property limits granice kamery; `null` przed pomiarem mapy
 * @property scale skala animacji systemu, zob. [rememberAnimatorScale]
 */
internal data class PickView(
    val from: MapCamera,
    val outline: CampusBounds,
    val free: FreeMap,
    val limits: MapLimits?,
    val scale: Float,
)

/** Kadr ruchu z [from] do [outline] w [free]; `null` bez konturu, widoku początkowego albo zmierzonej mapy. */
internal fun pickView(
    from: MapCamera?,
    outline: CampusBounds?,
    free: FreeMap,
    limits: MapLimits?,
    scale: Float,
): PickView? =
    if (outline == null || from == null || !free.isMeasured) {
        null
    } else {
        PickView(from = from, outline = outline, free = free, limits = limits, scale = scale)
    }
