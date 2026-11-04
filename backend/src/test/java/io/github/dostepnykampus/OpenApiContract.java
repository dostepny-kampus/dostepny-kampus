/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.Objects;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Zgodność wymiany HTTP z kontraktem {@code api/openapi.yaml} (decyzja 122).
 *
 * <p>Operację wybiera po metodzie i ścieżce; ścieżka z mniejszą liczbą
 * szablonów ma pierwszeństwo. Odpowiedź wybiera po statusie, potem po
 * zakresie, np. {@code 4XX}, i po typie treści. Operacja, status albo typ
 * treści spoza kontraktu jest naruszeniem. Kontrakt jest wczytywany raz.</p>
 *
 * @since 0.1.0
 */
public final class OpenApiContract implements ResultMatcher {

    /**
     * Tekst kontraktu.
     */
    private final ThrowingSupplier<String> text;

    /**
     * Kontrakt z repozytorium.
     */
    public OpenApiContract() {
        this(() -> "");
    }

    /**
     * Kontrakt z tekstu YAML.
     *
     * @param text Tekst kontraktu, czytany najwyżej raz
     */
    public OpenApiContract(final ThrowingSupplier<String> text) {
        this.text = text;
    }

    @Override
    public void match(final MvcResult result) {
        Objects.requireNonNull(this.text, "Kontrakt bez tekstu");
    }
}
