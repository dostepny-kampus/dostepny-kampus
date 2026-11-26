/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Wynik odświeżenia tokenu dostępu. */
internal sealed interface Renewal {
    /** Nowe tokeny w stanie [login]. */
    data class Fresh(val login: AuthState) : Renewal

    /** Usługa logowania odrzuciła token odświeżania albo go nie ma. */
    data object Rejected : Renewal

    /** Usługa logowania jest nieosiągalna; można spróbować później. */
    data object Unreachable : Renewal
}

/** Odświeżenie tokenu dostępu tokenem odświeżania. */
internal fun interface LoginRefresh {
    /** Odświeża tokeny [login]. */
    suspend fun refresh(login: AuthState): Renewal
}

/** Odświeżenie w Usłudze logowania przez AppAuth. Wymagania: US14.4 */
@Singleton
internal class AppAuthRefresh
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : LoginRefresh {
        private val service by lazy { AuthorizationService(context, loginConfiguration()) }

        override suspend fun refresh(login: AuthState): Renewal {
            if (login.refreshToken == null) return Renewal.Rejected
            return suspendCancellableCoroutine { done ->
                service.performTokenRequest(login.createTokenRefreshRequest()) { tokens, error ->
                    val renewal =
                        when {
                            tokens != null -> Renewal.Fresh(login.apply { update(tokens, null) })
                            error?.type == AuthorizationException.TYPE_GENERAL_ERROR -> Renewal.Unreachable
                            else -> Renewal.Rejected
                        }
                    done.resume(renewal)
                }
            }
        }
    }
