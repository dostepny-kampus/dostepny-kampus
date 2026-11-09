/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import io.github.dostepnykampus.feature.capture.navigation.ReportRoute

/**
 * Czynności V4 przekazywane z nawigatora aplikacji.
 *
 * @property onNext następny krok z danymi tego kroku
 * @property onSent zapisane w kolejce; dalej potwierdzenie
 * @property onBack „Wstecz” w nagłówku
 * @property onBackToMap „Wróć do mapy” po wysłaniu
 */
data class QuickReportActions(
    val onNext: (ReportRoute) -> Unit,
    val onSent: () -> Unit,
    val onBack: () -> Unit,
    val onBackToMap: () -> Unit,
)
