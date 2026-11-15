/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import io.github.dostepnykampus.feature.capture.photo.PhotoCamera

/**
 * Czynności C7.
 *
 * @property camera aparat „Zrób zdjęcie”
 * @property onSend „Wyślij do sprawdzenia” i „Spróbuj ponownie”
 * @property onBack „Wstecz”
 */
data class RemovedActions(val camera: PhotoCamera, val onSend: () -> Unit, val onBack: () -> Unit)
