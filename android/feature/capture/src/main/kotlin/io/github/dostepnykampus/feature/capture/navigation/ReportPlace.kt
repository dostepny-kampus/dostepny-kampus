/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import io.github.dostepnykampus.core.model.room.RoomCategory
import kotlinx.serialization.Serializable

/**
 * Obiekt planu wybrany przed V4 w arkuszu znaku: schody, toaleta dostosowana albo winda. Miejscem zgłoszenia jest
 * jego punkt, a piętrem piętro oglądanego planu.
 *
 * Wymagania: US4.6, BR-8, BR-14
 *
 * @property category rodzaj obiektu
 * @property buildingSymbol budynek obiektu
 * @property floorId piętro planu, na którym osoba dotknęła znaku
 * @property floorLabel etykieta tego piętra z danych PWr
 * @property latitude szerokość geograficzna punktu obiektu
 * @property longitude długość geograficzna punktu obiektu
 */
@Serializable
data class ReportPlace(
    val category: RoomCategory,
    val buildingSymbol: String,
    val floorId: String,
    val floorLabel: String,
    val latitude: Double,
    val longitude: Double,
)
