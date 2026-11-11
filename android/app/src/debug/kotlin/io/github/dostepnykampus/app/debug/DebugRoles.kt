/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app.debug

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.app.R
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import io.github.dostepnykampus.core.model.Account
import io.github.dostepnykampus.core.model.StaffRole
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Konto wybrane w przełączniku wersji debug (US14.1). Po wyborze roli ustawia je [DebugSignIn] z ról w tokenie
 * użytkownika testowego. Konto jest stanem Compose, więc zmiana roli od razu odświeża pasek i ekrany.
 */
@TymczasoweRole
@Singleton
class DebugRoles
    @Inject
    constructor() : RoleProvider {
        /** Bieżące konto; na starcie Gość. */
        var account: Account by mutableStateOf(Account.Guest)

        override fun currentAccount(): Account = account
    }

/**
 * Użytkownik testowy realmu deweloperskiego z `infra/keycloak/realm.json`.
 *
 * @property login nazwa użytkownika
 * @property password hasło użytkownika testowego, tylko w wersji debug
 */
class DebugUser(val login: String, val password: String) {
    /** Bez hasła, gdyby obiekt trafił do logów (NFR-35). */
    override fun toString(): String = "DebugUser($login)"
}

/**
 * Rola do wyboru w przełączniku: Gość, Odwiedzający i dwie role pracownicze (BR-1).
 *
 * @property label napis przycisku
 * @property account konto tej roli, jakie daje token jej użytkownika testowego
 * @property user użytkownik testowy tej roli; `null` dla Gościa
 */
enum class DebugRole(
    @param:StringRes val label: Int,
    val account: Account,
    val user: DebugUser?,
) {
    /** Gość niezalogowany (RO-GOS). */
    GUEST(R.string.debug_rola_gosc, Account.Guest, null),

    /** Odwiedzający (RO-ODW). */
    VISITOR(
        R.string.debug_rola_odwiedzajacy,
        Account.SignedIn(),
        DebugUser(login = "odwiedzajacy.test", password = "haslo-testowe-odwiedzajacy"),
    ),

    /** Zbierający (RO-ZBI). */
    COLLECTOR(
        R.string.debug_rola_zbierajacy,
        Account.SignedIn(setOf(StaffRole.COLLECTOR)),
        DebugUser(login = "zbierajacy.test", password = "haslo-testowe-zbierajacy"),
    ),

    /** Weryfikator (RO-WER). */
    VERIFIER(
        R.string.debug_rola_weryfikator,
        Account.SignedIn(setOf(StaffRole.VERIFIER)),
        DebugUser(login = "weryfikator.test", password = "haslo-testowe-weryfikator"),
    ),
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
