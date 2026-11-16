/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

/**
 * Czynności L1, które prowadzą poza ekran.
 *
 * @property onBack „Wstecz”
 * @property onLeave „Nie teraz” i „Wróć do mapy”: powrót do mapy
 */
data class SignInActions(val onBack: () -> Unit, val onLeave: () -> Unit)
