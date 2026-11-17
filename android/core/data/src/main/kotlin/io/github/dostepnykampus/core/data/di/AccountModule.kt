/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.AccountSession
import io.github.dostepnykampus.core.data.account.AccountSignIn
import io.github.dostepnykampus.core.data.account.BrowserSignIn
import io.github.dostepnykampus.core.data.account.KeystoreCipher
import io.github.dostepnykampus.core.data.account.LoginCipher
import io.github.dostepnykampus.core.data.account.LoginVault
import io.github.dostepnykampus.core.data.account.RoleProvider
import java.io.File

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

    /** Klucz zapisu logowania z Android Keystore. Wymagania: NFR-35 */
    @Binds
    fun cipher(keystore: KeystoreCipher): LoginCipher

    companion object {
        /** Zapis logowania w katalogu bez kopii zapasowej. Wymagania: US14.4, NFR-35 */
        @Provides
        fun vault(
            @ApplicationContext context: Context,
            cipher: LoginCipher,
        ): LoginVault = LoginVault(File(context.noBackupFilesDir, "logowanie"), cipher)
    }
}
