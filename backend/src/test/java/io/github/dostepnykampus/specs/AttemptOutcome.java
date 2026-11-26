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

    /** Status odmowy, który kontrakt daje ostatniej próbie. */
    private int refusal;

    /**
     * Wynik bez próby.
     */
    public AttemptOutcome() {
        this.status = 0;
        this.refusal = 403;
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
     * Zapamiętuje status odpowiedzi na próbę, której kontrakt odmawia innym statusem niż 403.
     *
     * @param code Status odpowiedzi serwera
     * @param refused Status odmowy z kontraktu dla tej próby
     */
    public void record(final int code, final int refused) {
        this.status = code;
        this.refusal = refused;
    }

    /**
     * Status odmowy, który kontrakt daje ostatniej próbie.
     *
     * @return Status odmowy; 403, gdy krok próby nie podał innego
     */
    public int refusal() {
        return this.refusal;
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
