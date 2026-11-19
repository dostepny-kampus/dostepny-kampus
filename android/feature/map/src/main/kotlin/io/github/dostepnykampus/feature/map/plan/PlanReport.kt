/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.room.RoomCategory

/**
 * Zgłoszenie z arkusza znaku planu: schody, toaleta dostosowana albo winda. Miejscem jest punkt znaku, a piętrem
 * piętro oglądanego planu, także przy windzie obsługującej kilka pięter.
 *
 * Wymagania: US4.6, BR-8, BR-14
 *
 * @property category rodzaj obiektu
 * @property building symbol budynku
 * @property floorId piętro planu, na którym osoba dotknęła znaku
 * @property floorLabel etykieta tego piętra z danych PWr
 * @property point punkt znaku obiektu
 */
data class PlanReport(
    val category: RoomCategory,
    val building: String,
    val floorId: String,
    val floorLabel: String,
    val point: GeoPoint,
)
