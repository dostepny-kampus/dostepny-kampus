/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Raport naruszeń reguł ArchUnit do porównania w asercji testu.
 *
 * @since 0.1.0
 */
final class ArchViolations {

    /**
     * Klasa bez instancji.
     */
    private ArchViolations() {
    }

    /**
     * Raport naruszeń reguł na klasach.
     *
     * @param classes Sprawdzane klasy
     * @param rules Reguły
     * @return Opisy naruszeń wszystkich reguł po kolei; pusty napis, gdy reguły są spełnione
     */
    static String of(final JavaClasses classes, final ArchRule... rules) {
        return Stream.of(rules)
            .map(rule -> rule.evaluate(classes))
            .filter(EvaluationResult::hasViolation)
            .map(result -> result.getFailureReport().toString())
            .collect(Collectors.joining(System.lineSeparator()));
    }
}
