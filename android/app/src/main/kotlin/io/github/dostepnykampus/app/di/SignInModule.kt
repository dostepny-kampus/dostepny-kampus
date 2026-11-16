/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.AccountSignIn
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.data.account.UnavailableSignIn

/** Logowanie ekranu L1 w grafie Hilt. Wymagania: US14.2 */
@Module
@InstallIn(SingletonComponent::class)
object SignInModule {
    /** Logowanie bez Usługi logowania, do czasu AppAuth. */
    @OptIn(TymczasoweRole::class)
    @Provides
    fun signIn(): AccountSignIn = UnavailableSignIn
}
