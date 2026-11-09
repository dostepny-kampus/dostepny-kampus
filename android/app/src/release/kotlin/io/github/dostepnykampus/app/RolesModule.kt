/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.app

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.account.GuestRoleProvider
import io.github.dostepnykampus.core.data.account.RoleProvider
import io.github.dostepnykampus.core.data.account.TymczasoweRole
import javax.inject.Singleton

/** Role w wersji release: bez logowania w aplikacji każda osoba jest Gościem (RO-GOS, US1.2). */
@Module
@InstallIn(SingletonComponent::class)
object RolesModule {
    /** Gość niezalogowany; przełącznik ról jest tylko w wersji debug. */
    @OptIn(TymczasoweRole::class)
    @Provides
    @Singleton
    fun roles(): RoleProvider = GuestRoleProvider
}
