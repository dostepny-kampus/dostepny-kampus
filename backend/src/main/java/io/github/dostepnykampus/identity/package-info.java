/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł identity: profil zalogowanej osoby i zaproszenia do zespołu.
 *
 * <p>Obsługuje logowanie z rolą i zaproszenie osoby z wybraną rolą
 * przez Koordynatora. Konta i hasła trzyma Keycloak, moduł tylko czyta
 * role z tokenu.</p>
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
