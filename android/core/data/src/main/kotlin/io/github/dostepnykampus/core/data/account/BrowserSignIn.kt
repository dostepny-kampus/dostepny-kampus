/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logowanie w przeglądarce systemowej przez [LoginActivity]; token z Usługi logowania trafia do [session].
 * Nowa próba kończy poprzednią, która jeszcze czeka, wynikiem `false`.
 *
 * Wymagania: US14.2, US14.3, NFR-31, NFR-35
 */
@Singleton
internal class BrowserSignIn
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val session: AccountSession,
    ) : AccountSignIn {
        private val waiting = AtomicReference<CompletableDeferred<String?>?>(null)

        override suspend fun open(entry: SignInEntry): Boolean {
            val result = CompletableDeferred<String?>()
            waiting.getAndSet(result)?.complete(null)
            context.startActivity(LoginActivity.intent(context, entry))
            val token = result.await()
            return token != null && session.signIn(token)
        }

        /** Wynik z [LoginActivity]: token dostępu albo `null` po przerwaniu i błędzie. */
        fun finish(token: String?) {
            waiting.getAndSet(null)?.complete(token)
        }
    }
