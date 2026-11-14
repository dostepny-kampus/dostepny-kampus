/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Strefa czasu kampusu, w której ekran mapy liczy dzień sprawdzenia i synchronizacji. */
internal val CampusZone: ZoneId = ZoneId.of("Europe/Warsaw")

/** Zapis dnia na ekranach mapy, np. `7.3.2026`. */
internal val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT)
