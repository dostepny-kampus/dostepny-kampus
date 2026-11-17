/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.annotation.StringRes
import io.github.dostepnykampus.app.R
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole

/**
 * Rola do wyboru w przełączniku: Gość, Odwiedzający i trzy role pracownicze.
 *
 * Wymagania: BR-1
 *
 * @property label napis przycisku
 * @property account konto tej roli
 */
enum class DebugRole(
    @param:StringRes val label: Int,
    val account: Account,
) {
    /** Gość niezalogowany. Wymagania: RO-GOS */
    GUEST(R.string.debug_rola_gosc, Account.Guest),

    /** Odwiedzający. Wymagania: RO-ODW */
    VISITOR(R.string.debug_rola_odwiedzajacy, Account.SignedIn()),

    /** Zbierający. Wymagania: RO-ZBI */
    COLLECTOR(R.string.debug_rola_zbierajacy, Account.SignedIn(setOf(StaffRole.COLLECTOR))),

    /** Weryfikator. Wymagania: RO-WER */
    VERIFIER(R.string.debug_rola_weryfikator, Account.SignedIn(setOf(StaffRole.VERIFIER))),

    /** Koordynator. Wymagania: RO-KOO */
    COORDINATOR(R.string.debug_rola_koordynator, Account.SignedIn(setOf(StaffRole.COORDINATOR))),
}
