/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import io.github.dostepnykampus.shared.error.ConflictingState;
import io.github.dostepnykampus.shared.error.InvalidInput;
import io.github.dostepnykampus.shared.error.MissingResource;
import java.util.Map;
import java.util.function.Supplier;
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

    /**
     * Wyjątek do rzucenia według rodzaju ze ścieżki.
     */
    private final Map<String, Supplier<RuntimeException>> failures;

    /**
     * Punkt z czterema rodzajami błędu używanymi w testach.
     */
    FailingController() {
        this(
            Map.of(
                "niepoprawne",
                () -> new InvalidInput("Szerokość 91 jest poza zakresem WGS84"),
                "brak", () -> new MissingResource("Nie ma budynku Z-99"),
                "awaria", () -> new IllegalStateException("sekret-wewnetrzny-7f3a"),
                "konflikt",
                () -> new ConflictingState(
                    "Na serwerze jest nowsza wersja", "currentState",
                    new CurrentState(3, "IN_REVIEW")
                )
            )
        );
    }

    /**
     * Punkt z podanymi rodzajami błędu.
     *
     * @param failures Wyjątek do rzucenia dla każdego rodzaju
     */
    private FailingController(final Map<String, Supplier<RuntimeException>> failures) {
        this.failures = failures;
    }

    @GetMapping("/proba-bledow/{kind}")
    String fail(@PathVariable final String kind) {
        if (this.failures.containsKey(kind)) {
            throw this.failures.get(kind).get();
        }
        return kind;
    }

    /**
     * Bieżący stan zasobu zwracany przy konflikcie.
     *
     * @param version Wersja na serwerze
     * @param status Status na serwerze
     * @since 0.1.0
     */
    record CurrentState(int version, String status) {
    }
}
