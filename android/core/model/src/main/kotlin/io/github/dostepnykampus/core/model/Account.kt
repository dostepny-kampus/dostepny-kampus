/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/** Rola pracownicza osoby zalogowanej; osoba może mieć kilka. Wymagania: BR-1 */
public enum class StaffRole {
    /** Zbierający. Wymagania: RO-ZBI */
    COLLECTOR,

    /** Weryfikator. Wymagania: RO-WER */
    VERIFIER,

    /** Koordynator. Wymagania: RO-KOO */
    COORDINATOR,
}

/** Kim jest osoba w aplikacji: od tego zależą ekrany, które widzi. Wymagania: US14.1 */
public sealed interface Account {
    /** Gość niezalogowany: mapa i opisy budynków bez konta. Wymagania: RO-GOS, US1.2 */
    public data object Guest : Account

    /** Osoba zalogowana: Odwiedzający, a z [staffRoles] także role pracownicze. Wymagania: RO-ODW, BR-1 */
    public data class SignedIn(val staffRoles: Set<StaffRole> = emptySet()) : Account
}

/** Czy osoba jest zalogowana, czyli może zgłosić barierę i otworzyć konto L2. Wymagania: US14.2 */
public val Account.isSignedIn: Boolean
    get() = this is Account.SignedIn

/**
 * Czy osoba ma co najmniej jedną rolę pracowniczą; wtedy czwarta pozycja paska to „Konto i praca”.
 *
 * Wymagania: US14.1
 */
public val Account.hasStaffRole: Boolean
    get() = this is Account.SignedIn && staffRoles.isNotEmpty()
