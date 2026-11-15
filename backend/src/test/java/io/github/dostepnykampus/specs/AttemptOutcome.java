/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.spring.CucumberTestContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Status odpowiedzi na ostatnią próbę ze scenariusza, wspólny dla klas kroków
 * (krok „próba jest odrzucona” jest jeden); żyje tyle co scenariusz.
 *
 * @since 0.1.0
 */
@Component
@Scope(CucumberTestContext.SCOPE_CUCUMBER_GLUE)
public final class AttemptOutcome {

    /** Status odpowiedzi na ostatnią próbę; 0 do pierwszej próby. */
    private int status;

    /**
     * Wynik bez próby.
     */
    public AttemptOutcome() {
        this.status = 0;
    }

    /**
     * Zapamiętuje status odpowiedzi na próbę.
     *
     * @param code Status odpowiedzi serwera
     */
    public void record(final int code) {
        this.status = code;
    }

    /**
     * Status odpowiedzi na ostatnią próbę.
     *
     * @return Status odpowiedzi serwera albo 0 bez próby
     */
    public int status() {
        return this.status;
    }
}
