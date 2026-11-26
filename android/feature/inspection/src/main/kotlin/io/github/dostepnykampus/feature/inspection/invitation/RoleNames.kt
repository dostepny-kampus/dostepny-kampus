/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.invitation

import androidx.annotation.StringRes
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.feature.inspection.R

/** Nazwa roli [role] przy wyborze roli. Wymagania: US12.1, BR-2 */
@StringRes
internal fun nameOf(role: StaffRole): Int =
    when (role) {
        StaffRole.COLLECTOR -> R.string.zaproszenie_zbierajacy
        StaffRole.VERIFIER -> R.string.zaproszenie_weryfikator
        StaffRole.COORDINATOR -> R.string.zaproszenie_koordynator
    }

/** Nazwa roli [role] w dopełniaczu. Wymagania: US12.2 */
@StringRes
internal fun genitiveOf(role: StaffRole): Int =
    when (role) {
        StaffRole.COLLECTOR -> R.string.zaproszenie_zbierajacego
        StaffRole.VERIFIER -> R.string.zaproszenie_weryfikatora
        StaffRole.COORDINATOR -> R.string.zaproszenie_koordynatora
    }
