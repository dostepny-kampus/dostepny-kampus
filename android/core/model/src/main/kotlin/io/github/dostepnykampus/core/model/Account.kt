/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

/** Rola pracownicza osoby zalogowanej; osoba może mieć kilka (BR-1). */
enum class StaffRole {
    /** Zbierający (RO-ZBI). */
    COLLECTOR,

    /** Weryfikator (RO-WER). */
    VERIFIER,

    /** Koordynator (RO-KOO). */
    COORDINATOR,
}

/** Kim jest osoba w aplikacji: od tego zależą ekrany, które widzi (US14.1). */
sealed interface Account {
    /** Gość niezalogowany (RO-GOS): mapa i opisy budynków bez konta (US1.2). */
    data object Guest : Account

    /** Osoba zalogowana: Odwiedzający (RO-ODW), a z [staffRoles] także role pracownicze (BR-1). */
    data class SignedIn(val staffRoles: Set<StaffRole> = emptySet()) : Account
}

/** Czy osoba jest zalogowana, czyli może zgłosić barierę i otworzyć konto L2 (US14.2). */
val Account.isSignedIn: Boolean
    get() = this is Account.SignedIn

/** Czy osoba ma co najmniej jedną rolę pracowniczą; wtedy czwarta pozycja paska to „Konto i praca” (US14.1). */
val Account.hasStaffRole: Boolean
    get() = this is Account.SignedIn && staffRoles.isNotEmpty()
