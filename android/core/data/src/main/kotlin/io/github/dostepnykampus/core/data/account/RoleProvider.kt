/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Dostawca konta i ról osoby; od niego zależą ekrany i pozycje paska dolnego (US14.1). Konto zmienia się po
 * zalogowaniu i wylogowaniu, więc ekrany i wznowienie wysyłki je obserwują, a nie czytają raz.
 */
interface RoleProvider {
    /** Bieżące konto osoby i każda jego zmiana. */
    val account: StateFlow<Account>
}

/** Dostawca na czas bez logowania w aplikacji: każda osoba jest Gościem niezalogowanym (RO-GOS, US1.2). */
@TymczasoweRole
object GuestRoleProvider : RoleProvider {
    override val account: StateFlow<Account> = MutableStateFlow<Account>(Account.Guest).asStateFlow()
}
