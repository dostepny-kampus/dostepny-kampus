/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import io.github.dostepnykampus.core.model.CampusZone
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.ROOT)
private val MOMENT_FORMAT = DateTimeFormatter.ofPattern("d.M.yyyy, HH:mm", Locale.ROOT)

/** Zapis dnia na ekranach, np. `7.3.2026`. */
public fun campusDay(day: LocalDate): String = DAY_FORMAT.format(day)

/** Zapis dnia, w którym wypadła chwila [moment] w strefie kampusu ([CampusZone]). */
public fun campusDay(moment: Instant): String = campusDay(moment.atZone(CampusZone).toLocalDate())

/** Zapis dnia i godziny chwili [moment] w strefie kampusu ([CampusZone]), np. `12.11.2026, 10:42`. */
public fun campusMoment(moment: Instant): String = MOMENT_FORMAT.format(moment.atZone(CampusZone))
