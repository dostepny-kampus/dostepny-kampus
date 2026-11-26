/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.account

import androidx.annotation.StringRes
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.model.StaffRole

/** Nazwa roli pracowniczej ze słownika użytkownika, np. „Zbierający”. Wymagania: BR-1, BR-5 */
@StringRes
public fun StaffRole.nameRes(): Int =
    when (this) {
        StaffRole.COLLECTOR -> R.string.rola_zbierajacy
        StaffRole.VERIFIER -> R.string.rola_weryfikator
        StaffRole.COORDINATOR -> R.string.rola_koordynator
    }
