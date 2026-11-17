/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.network.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token logowania konta, trzymany tylko w pamięci procesu. Klient HTTP dołącza go do żądań do serwera
 * aplikacji, które według kontraktu przyjmują logowanie; odpowiedź 401 serwera trafia do [rejections].
 *
 * Wymagania: NFR-35
 */
@Singleton
public class AccessTokens
    @Inject
    constructor() {
        private val current = MutableStateFlow<String?>(null)

        private val rejected = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        /** Bieżący token albo `null`, gdy konto nie jest zalogowane. */
        public val token: String?
            get() = current.value

        /** Identyfikator aktualnie zalogowanego konta. */
        public val subject: String?
            get() = token?.let(::subjectOf)

        /** Zmiany zalogowanego konta, także wylogowanie. */
        public val subjects: Flow<String?> = current.map { it?.let(::subjectOf) }.distinctUntilChanged()

        /** Odpowiedzi 401 serwera aplikacji: logowanie wygasło albo żądanie go wymaga. */
        public val rejections: SharedFlow<Unit> = rejected.asSharedFlow()

        /** Zapisuje token po zalogowaniu. */
        public fun signIn(token: String) {
            current.value = token
        }

        /** Usuwa token przy wylogowaniu. */
        public fun signOut() {
            current.value = null
        }

        internal fun reject() {
            rejected.tryEmit(Unit)
        }

        /** Token nie trafia do logów przez `toString()`. Wymagania: NFR-35 */
        override fun toString(): String = "AccessTokens(signedIn=${token != null})"
    }
