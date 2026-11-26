/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.dostepnykampus.core.data.invitation.Invitations
import io.github.dostepnykampus.core.data.invitation.ServerInvitations
import io.github.dostepnykampus.core.network.invitation.InvitationsApi

/** Zaproszenia Koordynatora w grafie Hilt aplikacji. */
@Module
@InstallIn(SingletonComponent::class)
internal object InvitationDataModule {
    /** Zaproszenie osoby z rolą przez serwer. Wymagania: US12.1, US12.2 */
    @Provides
    fun invitations(api: InvitationsApi): Invitations = ServerInvitations(api::invite)
}
