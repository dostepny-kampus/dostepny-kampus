/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.auth.realmRolesOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sesja konta: konto wynika z ról w tokenie Usługi logowania, a token trafia do [AccessTokens]. Na starcie Gość.
 * Podpis tokenu sprawdza serwer; aplikacja czyta role tylko po to, żeby dobrać ekrany.
 *
 * Wymagania: US14.1, BR-1, NFR-35
 *
 * @param tokens token dołączany do żądań do serwera
 */
@Singleton
public class AccountSession
    @Inject
    internal constructor(private val tokens: AccessTokens) : RoleProvider {
        private val state = MutableStateFlow<Account>(Account.Guest)

        override val account: StateFlow<Account> = state.asStateFlow()

        /**
         * Loguje konto tokenem dostępu. Tokenu, z którego nie da się odczytać konta, sesja nie przyjmuje.
         *
         * @param token token JWT z Usługi logowania
         * @return `true`, gdy konto jest zalogowane tym tokenem
         */
        public fun signIn(token: String): Boolean {
            val signedIn = accountOf(token) ?: return false
            tokens.signIn(token)
            state.value = signedIn
            return true
        }

        /** Wylogowuje: usuwa token i wraca do Gościa. */
        public fun signOut() {
            tokens.signOut()
            state.value = Account.Guest
        }
    }

/**
 * Konto z ról realmu w tokenie; token bez ról pracowniczych to Odwiedzający.
 *
 * @param token token JWT z Usługi logowania
 * @return konto albo `null`, gdy tokenu nie da się odczytać
 */
public fun accountOf(token: String): Account? =
    realmRolesOf(token)?.let { names -> Account.SignedIn(StaffRole.entries.filter { it.name in names }.toSet()) }
