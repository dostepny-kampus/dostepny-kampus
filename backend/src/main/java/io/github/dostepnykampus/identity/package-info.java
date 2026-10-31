/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł identity: profil zalogowanej osoby i zaproszenia do zespołu.
 *
 * <p>Obsługuje logowanie z rolą (US14) i zaproszenie osoby z wybraną rolą
 * przez Koordynatora (US12). Konta i hasła trzyma Keycloak, moduł tylko czyta
 * role z tokenu.</p>
 *
 * @since 0.1.0
 */
package io.github.dostepnykampus.identity;
