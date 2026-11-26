/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.account

/** Którą stronę Usługi logowania otworzyć. Wymagania: US14.2, US14.3 */
public enum class SignInEntry {
    /** „Zaloguj się”: strona logowania. */
    SIGN_IN,

    /** „Załóż konto”: strona rejestracji z klauzulą informacyjną. */
    REGISTER,
}

/**
 * Logowanie konta w Usłudze logowania, w przeglądarce systemowej; aplikacja nie widzi hasła.
 *
 * Wymagania: US14.2, US14.3, NFR-31
 */
public fun interface AccountSignIn {
    /**
     * Otwiera stronę [entry] i czeka, aż osoba skończy albo przerwie.
     *
     * @param entry logowanie albo rejestracja
     * @return `true`, gdy konto jest zalogowane; `false` po przerwaniu, błędzie sieci albo odmowie
     */
    public suspend fun open(entry: SignInEntry): Boolean
}
