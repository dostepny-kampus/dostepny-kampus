/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.AccountSession
import io.github.dostepnykampus.core.data.account.AccountSignIn
import io.github.dostepnykampus.core.data.account.BrowserSignIn
import io.github.dostepnykampus.core.data.account.RoleProvider

/** Konto `core:data` w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal interface AccountModule {
    /** Rolę dla ekranów daje sesja konta, w wersji release i debug. Wymagania: US14.1 */
    @Binds
    fun roles(session: AccountSession): RoleProvider

    /** „Zaloguj się” i „Załóż konto” otwierają Usługę logowania w przeglądarce. Wymagania: US14.2, US14.3 */
    @Binds
    fun signIn(browser: BrowserSignIn): AccountSignIn
}
