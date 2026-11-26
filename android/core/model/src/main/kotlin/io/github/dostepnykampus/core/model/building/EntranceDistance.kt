/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

/**
 * Wejście z odległością od punktu zgłoszenia, do opisu miejsca słowami.
 *
 * Wymagania: US7.4, NFR-19
 *
 * @property entrance wejście z danych
 * @property meters odległość w metrach na ziemi od punktu do wejścia
 */
public data class EntranceDistance(val entrance: Entrance, val meters: Double)
