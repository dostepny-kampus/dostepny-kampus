/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł identity: profil zalogowanej osoby, zabezpieczenie API i konta w Usłudze logowania.
 *
 * <p>Zadanie: role z tokenu Keycloak, dostęp do API według ról i nadawanie ról kontom.</p>
 *
 * <p>API: brak typów dla innych modułów; operacja HTTP {@code /me}.</p>
 *
 * <p>Zależności: brak.</p>
 *
 * <p>Nie robi: nie przechowuje kont ani haseł (Keycloak) i nie wysyła e-maili.</p>
 *
 * <p>Kiedy dodawać tutaj: odczyt tożsamości i ról; dostęp operacji zostaje przy operacji.</p>
 *
 * <p>Wymagania: US14, US12</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {})
@NullMarked
package io.github.dostepnykampus.identity;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
