/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import io.github.dostepnykampus.core.network.auth.AccessTokens
import io.github.dostepnykampus.core.network.auth.accountClaimsOf
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
import kotlin.coroutines.CoroutineContext

/**
 * Sesja konta odczytuje role i nazwę z tokenu Usługi logowania, a token trafia do [AccessTokens]. Na starcie Gość,
 * dopóki nie wróci zapisane logowanie; jego token dostępu odświeża się przed wygaśnięciem.
 *
 * Wymagania: US14.1, US14.4, BR-1, NFR-35
 *
 * @param tokens token dołączany do żądań do serwera
 * @param vault zalogowanie zapisane do następnego uruchomienia
 * @param refresher odświeżenie tokenu dostępu
 * @param work wątek zapisu i odświeżania
 * @param queue wpisy kolejki w telefonie, które sesja przekazuje zalogowanemu kontu
 */
public class AccountSession
    internal constructor(
        private val tokens: AccessTokens,
        private val vault: LoginVault,
        private val refresher: LoginRefresh,
        private val work: CoroutineContext,
        private val queue: QueueHandover,
    ) : RoleProvider {
        private val state = MutableStateFlow<Account>(Account.Guest)
        private val scope = CoroutineScope(SupervisorJob() + work)
        private val renewal = AtomicReference<Job?>(null)

        override val account: StateFlow<Account> = state.asStateFlow()

        private val restored = scope.launch { vault.read()?.takeIf { state.value == Account.Guest }?.let { keep(it) } }

        internal suspend fun awaitRestore() = restored.join()

        /**
         * Loguje konto stanem logowania z Usługi logowania i zapisuje go do następnego uruchomienia.
         *
         * @return `true`, gdy konto jest zalogowane tokenem dostępu z [login]
         */
        internal suspend fun signIn(login: AuthState): Boolean {
            if (!keep(login)) return false
            withContext(work) { vault.write(login) }
            queue.onLoggedIn()
            return true
        }

        /**
         * Loguje konto tokenem dostępu. Tokenu, z którego nie da się odczytać konta, sesja nie przyjmuje.
         *
         * @param token token JWT z Usługi logowania
         * @return `true`, gdy konto jest zalogowane tym tokenem
         */
        public suspend fun signIn(token: String): Boolean = activate(token).also { if (it) queue.onLoggedIn() }

        private suspend fun activate(token: String): Boolean {
            val signedIn = accountOf(token) ?: return false
            val subject = subjectOf(token)
            if (subject != null && subject != tokens.subject) {
                withContext(work) { queue.adopt(subject) }
            }
            tokens.signIn(token)
            state.value = signedIn
            return true
        }

        /** Wylogowuje: usuwa token i zapisane logowanie, wraca do Gościa. */
        override fun signOut() {
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

/** Wpisy kolejki w telefonie, które sesja konta przekazuje zalogowanemu kontu. Wymagania: US14.1, BR-1 */
internal interface QueueHandover {
    /** Przypisuje kontu [subject] wpisy kolejki bez właściciela. */
    suspend fun adopt(subject: String)

    /** Budzi wysyłkę kolejki po zalogowaniu konta. */
    fun onLoggedIn()
}

/**
 * Konto z rolami realmu i nazwą osoby z tokenu. Bez ról pracowniczych konto ma rolę Odwiedzającego.
 *
 * @param token token JWT z Usługi logowania
 * @return konto albo `null`, gdy tokenu nie da się odczytać
 */
public fun accountOf(token: String): Account? {
    val claims = accountClaimsOf(token) ?: return null
    return Account.SignedIn(
        staffRoles = StaffRole.entries.filter { it.name in claims.realmRoles }.toSet(),
        displayName = claims.displayName,
    )
}
