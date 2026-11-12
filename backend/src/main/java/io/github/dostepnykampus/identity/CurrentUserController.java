/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Profil zalogowanej osoby do ekranu profilu i wyboru ekranów roli
 * (US14.1, BR-1). Dane pochodzą z tokenu; serwer nie pyta Usługi logowania.
 *
 * @since 0.1.0
 */
@RestController
public final class CurrentUserController {

    /**
     * Tworzy kontroler; konstruktor wywołuje tylko Spring przez refleksję.
     */
    private CurrentUserController() {
    }

    /**
     * Zwraca identyfikator, nazwę, adres i role zalogowanej osoby.
     *
     * @param account Zalogowane konto z tokenem
     * @return Profil według schematu CurrentUser
     */
    @GetMapping("/me")
    static CurrentUserResponse getCurrentUser(final JwtAuthenticationToken account) {
        return new CurrentUserResponse(account);
    }
}
