/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import io.github.dostepnykampus.core.model.inspection.MarkerPlace

/**
 * Czynności przy wierszu piętra w C5 Zbierającego.
 *
 * Wymagania: US15.1, US15.2
 *
 * @property onMark „Oznacz jako obejrzane” i „Spróbuj ponownie” po błędzie zapisu: miejsce znacznika
 * @property onAsk „Cofnij znacznik”: identyfikator własnego znacznika
 * @property onKeep „Zostaw znacznik”
 * @property onConfirm „Cofnij znacznik” w potwierdzeniu
 */
data class CollectorMarkerActions(
    val onMark: (MarkerPlace) -> Unit = {},
    val onAsk: (markerId: String) -> Unit = {},
    val onKeep: () -> Unit = {},
    val onConfirm: () -> Unit = {},
)
