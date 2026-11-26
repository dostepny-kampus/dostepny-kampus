/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute

/**
 * Czynności ekranu C3.
 *
 * Wymagania: US8.3, US9.3, US17.2, BR-23
 *
 * @property onBack „Wstecz”
 * @property onMeasure „Zmierz i zgłoś barierę”: C1 (PW-12)
 * @property onOpen „Otwórz” przy zgłoszeniu do poprawki w C2 (bez przyjętych i archiwalnych); `null` ukrywa
 *   przycisk wszędzie
 * @property onConflict wybór wersji w C4 przy zgłoszeniu z konfliktem wersji; `null` ukrywa znak wszędzie
 * @property onOpenMarking „Otwórz” i „Wyślij ponownie” przy zmianie oznaczenia wejścia: C6 z tą trasą; `null`
 *   ukrywa przyciski przy zmianach oznaczenia
 * @property onOpenRemoval „Otwórz” przy zgłoszeniu „bariera usunięta”: C7 z jego identyfikatorem; `null` ukrywa
 *   przycisk przy takich zgłoszeniach
 * @property onResendChange „Wyślij ponownie” przy nieaktualnej zmianie bariery: C2 z jej treścią jako nowa zmiana
 *   bieżącego przyjętego zgłoszenia; `null` ukrywa przycisk przy takich zmianach
 * @property onSync „Czeka na wysłanie: N”: C4 bez konfliktu; `null` zostawia sam napis
 */
data class MySubmissionsActions(
    val onBack: () -> Unit,
    val onMeasure: () -> Unit,
    val onOpen: ((id: String) -> Unit)? = null,
    val onConflict: ((id: String) -> Unit)? = null,
    val onOpenMarking: ((route: MarkingRoute) -> Unit)? = null,
    val onOpenRemoval: ((id: String) -> Unit)? = null,
    val onResendChange: ((id: String) -> Unit)? = null,
    val onSync: (() -> Unit)? = null,
)
