/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.data.di.IoContext
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.database.QueueOwnershipDao
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.auth.realmRolesOf
import io.github.dostepnykampus.core.network.auth.subjectOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AuthState
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Sesja konta: konto wynika z ról w tokenie Usługi logowania, a token trafia do [AccessTokens]. Na starcie Gość,
 * dopóki nie wróci zapisane logowanie; jego token dostępu odświeża się przed wygaśnięciem.
 *
 * Wymagania: US14.1, US14.4, BR-1, NFR-35
 *
 * @param tokens token dołączany do żądań do serwera
 * @param vault zalogowanie zapisane do następnego uruchomienia
 * @param refresher odświeżenie tokenu dostępu
 * @param work wątek zapisu i odświeżania
 * @param adoptLegacyQueue przypisanie wpisów bez właściciela
 * @param onLogin wywołanie po zalogowaniu konta
 */
@Singleton
public class AccountSession
    internal constructor(
        private val tokens: AccessTokens,
        private val vault: LoginVault,
        private val refresher: LoginRefresh,
        private val work: CoroutineContext,
        private val adoptLegacyQueue: suspend (String) -> Unit = {},
        private val onLogin: () -> Unit = {},
    ) : RoleProvider {
        private val state = MutableStateFlow<Account>(Account.Guest)
        private val scope = CoroutineScope(SupervisorJob() + work)
        private val renewal = AtomicReference<Job?>(null)

        override val account: StateFlow<Account> = state.asStateFlow()

        private val restored = scope.launch { vault.read()?.takeIf { state.value == Account.Guest }?.let { keep(it) } }

        @Inject
        internal constructor(
            tokens: AccessTokens,
            vault: LoginVault,
            refresher: LoginRefresh,
            @IoContext work: CoroutineContext,
            owners: QueueOwnershipDao,
            queue: SubmissionQueue,
        ) : this(tokens, vault, refresher, work, owners::adopt, queue::onLoggedIn)

        internal suspend fun awaitRestore(): Unit = restored.join()

        /**
         * Loguje konto stanem logowania z Usługi logowania i zapisuje go do następnego uruchomienia.
         *
         * @return `true`, gdy konto jest zalogowane tokenem dostępu z [login]
         */
        internal suspend fun signIn(login: AuthState): Boolean {
            if (!keep(login)) return false
            withContext(work) { vault.write(login) }
            onLogin()
            return true
        }

        /**
         * Loguje konto tokenem dostępu. Tokenu, z którego nie da się odczytać konta, sesja nie przyjmuje.
         *
         * @param token token JWT z Usługi logowania
         * @return `true`, gdy konto jest zalogowane tym tokenem
         */
        public suspend fun signIn(token: String): Boolean = activate(token).also { if (it) onLogin() }

        private suspend fun activate(token: String): Boolean {
            val signedIn = accountOf(token) ?: return false
            val subject = subjectOf(token)
            if (subject != null && subject != tokens.subject) {
                withContext(work) { adoptLegacyQueue(subject) }
            }
            tokens.signIn(token)
            state.value = signedIn
            return true
        }

        /** Wylogowuje: usuwa token i zapisane logowanie, wraca do Gościa. */
        public fun signOut() {
            renewal.getAndSet(null)?.cancel()
            tokens.signOut()
            state.value = Account.Guest
            scope.launch { vault.clear() }
        }

        private suspend fun keep(login: AuthState): Boolean {
            val token = login.accessToken
            if (token == null || !activate(token)) return false
            val job = scope.launch(start = CoroutineStart.LAZY) { renew(login) }
            renewal.getAndSet(job)?.cancel()
            job.start()
            return true
        }

        private suspend fun renew(login: AuthState) {
            val expiry = login.accessTokenExpirationTime ?: return
            delay(expiry - System.currentTimeMillis() - EARLY_MS)
            var next = refresher.refresh(login)
            while (next == Renewal.Unreachable) {
                delay(RETRY_MS)
                next = refresher.refresh(login)
            }
            if (next is Renewal.Fresh && keep(next.login)) vault.write(next.login) else signOut()
        }

        private companion object {
            const val EARLY_MS = 60_000L
            const val RETRY_MS = 30_000L
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
