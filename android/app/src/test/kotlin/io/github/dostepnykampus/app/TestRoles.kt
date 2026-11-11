/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.model.Account
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Dostawca konta w testach, oparty na zwykłym [MutableStateFlow], bez stanu Compose: test sprawdza, że ekrany
 * obserwują konto przez kontrakt [RoleProvider], a nie przez szczegół implementacji.
 *
 * @param start konto na starcie
 */
internal class TestRoles(start: Account = Account.Guest) : RoleProvider {
    override val account = MutableStateFlow(start)
}
