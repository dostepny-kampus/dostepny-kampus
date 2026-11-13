/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł identity: profil zalogowanej osoby i zabezpieczenie API.
 *
 * <p>Zadanie: role zalogowanej osoby z tokenu Keycloak i dostęp do
 * operacji API według tych ról.</p>
 *
 * <p>API: brak typów dla innych modułów; operacja HTTP {@code /me}.</p>
 *
 * <p>Zależności: brak.</p>
 *
 * <p>Nie robi: nie przechowuje kont ani haseł (trzyma je Keycloak) i nie
 * zaprasza osób do zespołu.</p>
 *
 * <p>Kiedy dodawać tutaj: kod, który czyta tożsamość albo role zalogowanej
 * osoby. Reguła dostępu jednej operacji zostaje w module tej operacji.</p>
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
