/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import java.time.ZoneId

/** Strefa czasu kampusu: w niej liczy się dzień sprawdzenia i synchronizacji, nie w strefie telefonu. */
public val CampusZone: ZoneId = ZoneId.of("Europe/Warsaw")
