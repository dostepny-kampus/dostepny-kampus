/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account

/** Dostawca konta i ról osoby; od niego zależą ekrany i pozycje paska dolnego (US14.1). */
fun interface RoleProvider {
    /** Bieżące konto osoby. */
    fun currentAccount(): Account
}

/** Dostawca na czas bez logowania w aplikacji: każda osoba jest Gościem niezalogowanym (US1.2, US14.2). */
object GuestRoleProvider : RoleProvider {
    override fun currentAccount(): Account = Account.Guest
}
