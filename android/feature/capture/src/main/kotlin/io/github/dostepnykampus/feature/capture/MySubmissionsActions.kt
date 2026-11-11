/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

/**
 * Czynności ekranu C3.
 *
 * @property onBack „Wstecz”
 * @property onMeasure „Zmierz i zgłoś barierę”: C1 (PW-12)
 * @property onOpen „Otwórz” przy zgłoszeniu z telefonu: poprawka w C2 (US8.3); wysłane go nie mają, bo ich treść jest
 *   tylko na serwerze; `null` ukrywa przycisk wszędzie
 */
data class MySubmissionsActions(
    val onBack: () -> Unit,
    val onMeasure: () -> Unit,
    val onOpen: ((id: String) -> Unit)? = null,
)
