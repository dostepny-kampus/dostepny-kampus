/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.invitation

/**
 * Czynności K2, które prowadzą poza ekran.
 *
 * @property onBack „Wstecz”
 * @property onLeave „Wróć do mapy” przy braku uprawnień
 */
data class InvitationActions(val onBack: () -> Unit, val onLeave: () -> Unit)
