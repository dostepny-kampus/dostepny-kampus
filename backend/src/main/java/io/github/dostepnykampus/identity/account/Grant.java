/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account;

/**
 * Wynik nadania roli kontu z adresem e-mail.
 *
 * <p>Wymagania: US12.1, US12.2</p>
 *
 * @since 0.1.0
 */
public enum Grant {
    /** Konta nie było: powstało z rolą Odwiedzającego i nadaną rolą. */
    CREATED,

    /** Konto istniało i dostało rolę obok dotychczasowych. */
    ADDED,

    /** Konto istniało i miało już tę rolę; nic się nie zmieniło. */
    UNCHANGED
}
