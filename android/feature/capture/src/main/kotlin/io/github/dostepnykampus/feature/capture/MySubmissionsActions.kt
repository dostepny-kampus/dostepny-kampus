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
 * @property onOpen „Otwórz” przy zgłoszeniu: poprawka w C2 (US8.3)
 * @property onMeasure „Zmierz i zgłoś barierę”: C1 (PW-12)
 */
data class MySubmissionsActions(val onBack: () -> Unit, val onOpen: (id: String) -> Unit, val onMeasure: () -> Unit)
