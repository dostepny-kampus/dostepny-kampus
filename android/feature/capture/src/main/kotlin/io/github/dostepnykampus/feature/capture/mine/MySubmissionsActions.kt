/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

/**
 * Czynności ekranu C3.
 *
 * Wymagania: US8.3, US9.3
 *
 * @property onBack „Wstecz”
 * @property onMeasure „Zmierz i zgłoś barierę”: C1 (PW-12)
 * @property onOpen „Otwórz” przy zgłoszeniu do poprawki w C2 (bez przyjętych i archiwalnych); `null` ukrywa
 *   przycisk wszędzie
 * @property onConflict wybór wersji w C4 przy zgłoszeniu z konfliktem wersji; `null` ukrywa znak wszędzie
 */
data class MySubmissionsActions(
    val onBack: () -> Unit,
    val onMeasure: () -> Unit,
    val onOpen: ((id: String) -> Unit)? = null,
    val onConflict: ((id: String) -> Unit)? = null,
)
