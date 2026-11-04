/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Punkt HTTP tylko dla testów, rzucający wyjątek rodzaju podanego w ścieżce;
 * nieznany rodzaj wraca jako zwykła odpowiedź.
 *
 * <p>{@link TestComponent} wyłącza klasę ze skanowania; kontekst dostaje ją
 * tylko przez {@code @Import}.</p>
 *
 * @since 0.1.0
 */
@TestComponent
@RestController
final class FailingController {

    @GetMapping("/proba-bledow/{kind}")
    String fail(@PathVariable final String kind) {
        return switch (kind) {
            case "niepoprawne" -> throw new InvalidInput(
                "Szerokość 91 jest poza zakresem WGS84"
            );
            case "brak" -> throw new MissingResource("Nie ma budynku Z-99");
            case "awaria" -> throw new IllegalStateException("sekret-wewnetrzny-7f3a");
            default -> kind;
        };
    }
}
