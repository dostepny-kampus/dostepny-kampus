/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.annotation.StringRes
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.app.R
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Konto wybrane w przełączniku wersji debug. Po wyborze roli ustawia je [DebugSignIn] z ról w tokenie
 * użytkownika testowego. Zmiana konta od razu odświeża pasek i ekrany, które je obserwują.
 *
 * Wymagania: US14.1
 */
@TymczasoweRole
@Singleton
class DebugRoles
    @Inject
    constructor() : RoleProvider {
        private val state = MutableStateFlow<Account>(Account.Guest)

        /** Bieżące konto; na starcie Gość. */
        override val account: StateFlow<Account> = state.asStateFlow()

        /** Ustawia konto po zalogowaniu albo wylogowaniu w przełączniku. */
        fun switchTo(account: Account) {
            state.value = account
        }
    }

/**
 * Rola do wyboru w przełączniku: Gość, Odwiedzający i dwie role pracownicze.
 *
 * Wymagania: BR-1
 *
 * @property label napis przycisku
 * @property account konto tej roli
 */
enum class DebugRole(
    @param:StringRes val label: Int,
    val account: Account,
) {
    /** Gość niezalogowany. Wymagania: RO-GOS */
    GUEST(R.string.debug_rola_gosc, Account.Guest),

    /** Odwiedzający. Wymagania: RO-ODW */
    VISITOR(R.string.debug_rola_odwiedzajacy, Account.SignedIn()),

    /** Zbierający. Wymagania: RO-ZBI */
    COLLECTOR(R.string.debug_rola_zbierajacy, Account.SignedIn(setOf(StaffRole.COLLECTOR))),

    /** Weryfikator. Wymagania: RO-WER */
    VERIFIER(R.string.debug_rola_weryfikator, Account.SignedIn(setOf(StaffRole.VERIFIER))),
}

/** Role w wersji debug: dostawca z przełącznikiem zamiast stałego Gościa. */
@Module
@InstallIn(SingletonComponent::class)
object RolesModule {
    /** Ten sam [DebugRoles] dla aplikacji i dla przełącznika. */
    @OptIn(TymczasoweRole::class)
    @Provides
    fun roles(roles: DebugRoles): RoleProvider = roles
}
