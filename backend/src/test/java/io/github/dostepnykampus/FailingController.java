/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punkty HTTP tylko dla testów, rzucające wyjątek wybranego rodzaju.
 *
 * <p>{@link TestComponent} wyłącza klasę ze skanowania; kontekst dostaje ją
 * tylko przez {@code @Import}.</p>
 *
 * @since 0.1.0
 */
@TestComponent
@RestController
final class FailingController {

    @GetMapping("/proba-bledow/niepoprawne")
    void invalid() {
        throw new InvalidInput("Szerokość 91 jest poza zakresem WGS84");
    }

    @GetMapping("/proba-bledow/brak")
    void missing() {
        throw new MissingResource("Nie ma budynku Z-99");
    }

    @GetMapping("/proba-bledow/awaria")
    void broken() {
        throw new IllegalStateException("sekret-wewnetrzny-7f3a");
    }
}
