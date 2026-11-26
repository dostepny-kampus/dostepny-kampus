/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account
import kotlinx.coroutines.flow.StateFlow

/**
 * Dostawca konta i ról osoby; od niego zależą ekrany i pozycje paska dolnego. Konto zmienia się po
 * zalogowaniu i wylogowaniu, a ekrany obserwują te zmiany.
 *
 * Wymagania: US14.1, US14.4
 */
public interface RoleProvider {
    /** Bieżące konto osoby i każda jego zmiana. */
    public val account: StateFlow<Account>

    /** Wylogowuje: konto wraca do Gościa, a zapisane logowanie znika. */
    public fun signOut()
}
