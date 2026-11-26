/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.conflict

/**
 * Czynności C4 przy konflikcie wersji.
 *
 * @property onBack „Wstecz”; wybór czeka jak po „Zdecyduję później”
 * @property onKeepMine „Zostaw moją wersję”
 * @property onTakeServer „Weź wersję z serwera”
 * @property onLater „Zdecyduję później”
 */
data class ConflictActions(
    val onBack: () -> Unit,
    val onKeepMine: () -> Unit,
    val onTakeServer: () -> Unit,
    val onLater: () -> Unit,
)
