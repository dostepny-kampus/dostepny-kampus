/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token logowania konta, trzymany tylko w pamięci procesu (NFR-35). Klient HTTP dołącza go do żądań do serwera
 * aplikacji, które według kontraktu przyjmują logowanie; odpowiedź 401 serwera trafia do [rejections].
 */
@Singleton
class AccessTokens
    @Inject
    constructor() {
        @Volatile
        private var current: String? = null

        private val rejected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /** Bieżący token albo `null`, gdy konto nie jest zalogowane. */
        val token: String?
            get() = current

        /** Odpowiedzi 401 serwera aplikacji: logowanie wygasło albo żądanie go wymaga. */
        val rejections: SharedFlow<Unit> = rejected.asSharedFlow()

        /** Zapisuje token po zalogowaniu. */
        fun signIn(token: String) {
            current = token
        }

        /** Usuwa token przy wylogowaniu. */
        fun signOut() {
            current = null
        }

        internal fun reject() {
            rejected.tryEmit(Unit)
        }

        /** Token nie trafia do logów przez `toString()` (NFR-35). */
        override fun toString(): String = "AccessTokens(signedIn=${current != null})"
    }
