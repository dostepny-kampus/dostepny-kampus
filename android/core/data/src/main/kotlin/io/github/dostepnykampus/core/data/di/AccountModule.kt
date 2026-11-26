/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
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
import io.github.dostepnykampus.core.data.account.AppAuthRefresh
import io.github.dostepnykampus.core.data.account.BrowserSignIn
import io.github.dostepnykampus.core.data.account.KeystoreCipher
import io.github.dostepnykampus.core.data.account.LoginCipher
import io.github.dostepnykampus.core.data.account.LoginRefresh
import io.github.dostepnykampus.core.data.account.LoginVault
import io.github.dostepnykampus.core.data.account.QueueHandover
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.submission.sync.SubmissionQueue
import io.github.dostepnykampus.core.database.QueueOwnershipDao
import io.github.dostepnykampus.core.network.auth.AccessTokens
import java.io.File
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

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

    /** Token dostępu odświeża Usługa logowania. Wymagania: US14.4 */
    @Binds
    fun refresh(appAuth: AppAuthRefresh): LoginRefresh

    companion object {
        /** Zapis logowania w katalogu bez kopii zapasowej. Wymagania: US14.4, NFR-35 */
        @Provides
        fun vault(
            @ApplicationContext context: Context,
            cipher: LoginCipher,
        ): LoginVault = LoginVault(File(context.noBackupFilesDir, "logowanie"), cipher)

        /**
         * Logowanie innego konta przejmuje wpisy kolejki bez właściciela z [owners], a każde logowanie budzi
         * wysyłkę kolejki zgłoszeń [submissions]. Wymagania: US14.1, BR-1
         */
        @Provides
        fun queueHandover(owners: QueueOwnershipDao, submissions: SubmissionQueue): QueueHandover =
            object : QueueHandover {
                override suspend fun adopt(subject: String) = owners.adopt(subject)

                override fun onLoggedIn() = submissions.onLoggedIn()
            }

        /** Jedna sesja konta na proces. Wymagania: US14.1, US14.4 */
        @Provides
        @Singleton
        fun accountSession(
            tokens: AccessTokens,
            vault: LoginVault,
            refresher: LoginRefresh,
            @IoContext work: CoroutineContext,
            queue: QueueHandover,
        ): AccountSession =
            AccountSession(tokens = tokens, vault = vault, refresher = refresher, work = work, queue = queue)
    }
}
