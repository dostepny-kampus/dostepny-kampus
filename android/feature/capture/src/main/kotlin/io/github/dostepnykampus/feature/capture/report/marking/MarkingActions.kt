/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera

/**
 * Czynności C6.
 *
 * @property camera aparat „Zrób zdjęcie”
 * @property onChoose wybór nowego oznaczenia
 * @property onSend „Wyślij do sprawdzenia” i „Spróbuj ponownie”
 * @property onBack „Wstecz”
 */
data class MarkingActions(
    val camera: PhotoCamera,
    val onChoose: (EntranceMarking) -> Unit,
    val onSend: () -> Unit,
    val onBack: () -> Unit,
)
