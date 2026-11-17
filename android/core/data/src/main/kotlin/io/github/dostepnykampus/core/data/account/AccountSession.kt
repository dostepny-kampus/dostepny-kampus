/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.data.di.IoContext
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.auth.realmRolesOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Sesja konta: konto wynika z ról w tokenie Usługi logowania, a token trafia do [AccessTokens]. Na starcie Gość,
 * dopóki nie wróci logowanie zapisane do następnego uruchomienia.
 *
 * Wymagania: US14.1, US14.4, BR-1, NFR-35
 *
 * @param tokens token dołączany do żądań do serwera
 * @param vault zalogowanie zapisane do następnego uruchomienia
 * @param work wątek odczytu i zapisu
 */
@Singleton
public class AccountSession
    @Inject
    internal constructor(
        private val tokens: AccessTokens,
        private val vault: LoginVault,
        @param:IoContext private val work: CoroutineContext,
    ) : RoleProvider {
        private val state = MutableStateFlow<Account>(Account.Guest)
        private val scope = CoroutineScope(SupervisorJob() + work)

        override val account: StateFlow<Account> = state.asStateFlow()

        init {
            scope.launch { vault.read()?.takeIf { state.value == Account.Guest }?.let(::keep) }
        }

        /**
         * Loguje konto stanem logowania z Usługi logowania i zapisuje go do następnego uruchomienia.
         *
         * @return `true`, gdy konto jest zalogowane tokenem dostępu z [login]
         */
        internal suspend fun signIn(login: AuthState): Boolean {
            if (!keep(login)) return false
            withContext(work) { vault.write(login) }
            return true
        }

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

        /** Wylogowuje: usuwa token i zapisane logowanie, wraca do Gościa. */
        public fun signOut() {
            tokens.signOut()
            state.value = Account.Guest
            scope.launch { vault.clear() }
        }

        private fun keep(login: AuthState): Boolean {
            val token = login.accessToken
            return token != null && signIn(token)
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
