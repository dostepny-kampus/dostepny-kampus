/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/**
 * Budynek PWr na mapie kampusu (`Building` z kontraktu API).
 *
 * @property symbol symbol budynku, np. `C-13`; unikalny
 * @property name nazwa budynku
 * @property outline kontur budynku z OpenStreetMap
 */
data class Building(val symbol: String, val name: String, val outline: GeoArea)
