/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;

/**
 * Reguła architektury z listą znanych naruszeń. Reguła pomija pakiety z listy, a wpis zostaje
 * tylko, dopóki naruszenie istnieje, więc lista tylko maleje.
 *
 * @since 0.1.0
 */
final class KnownViolations {

    /**
     * Klasa bez instancji.
     */
    private KnownViolations() {
    }

    /**
     * Sprawdza regułę poza listą i to, że bez każdego wpisu reguła nie przechodzi.
     *
     * @param classes Klasy kodu produkcyjnego
     * @param packages Pakiety z listy wyjątków i issue, które usuwa naruszenie
     * @param rule Reguła pomijająca podane pakiety
     */
    static void check(
        final JavaClasses classes, final Map<String, Integer> packages,
        final Function<Set<String>, ArchRule> rule
    ) {
        Assertions.assertAll(
            () -> rule.apply(packages.keySet()).check(classes),
            () -> Assertions.assertEquals(
                List.of(),
                packages.keySet().stream().filter(
                    pack -> !rule.apply(KnownViolations.without(packages.keySet(), pack))
                        .evaluate(classes).hasViolation()
                ).toList(),
                "Wyjątek z listy już nie jest naruszeniem; usuń wpis"
            )
        );
    }

    private static Set<String> without(final Set<String> packages, final String pack) {
        return packages.stream().filter(other -> !other.equals(pack))
            .collect(Collectors.toUnmodifiableSet());
    }
}
