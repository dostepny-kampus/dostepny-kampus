/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * Operacja kontraktu {@code api/openapi.yaml}: nazwa "METODA ścieżka" i wymagania
 * {@code security} (#829).
 *
 * <p>Nazwa ma prefiks z {@code servers}, a każdą zmienną ścieżki zapisaną jako
 * {@code {}}, więc nazwy zmiennych, także z wyrażeniem regularnym Springa, nie mają
 * znaczenia. Jedno miejsce tej zamiany dla parytetu tras i testu zabezpieczeń.</p>
 *
 * @param name Metoda i znormalizowana ścieżka, np. "GET /api/buildings/{}"
 * @param security Wpis {@code security} operacji albo globalny
 * @since 0.1.0
 */
record ContractOperation(String name, JsonNode security) {

    /**
     * Zmienna w szablonie ścieżki, także z wyrażeniem regularnym Springa.
     */
    private static final Pattern VARIABLE = Pattern.compile("\\{[^}]*}");

    /**
     * Metody HTTP, które kontrakt może opisywać pod ścieżką.
     */
    private static final Set<String> VERBS = Set.of(
        "get", "put", "post", "delete", "patch", "head", "options", "trace"
    );

    /**
     * Czy klucz pod ścieżką kontraktu jest metodą HTTP, a nie np. {@code parameters}.
     *
     * @param key Klucz pod ścieżką
     * @return Prawda dla metody HTTP
     */
    static boolean verb(final String key) {
        return ContractOperation.VERBS.contains(key);
    }

    /**
     * Prefiks ścieżek z pierwszego wpisu {@code servers}, bez końcowego ukośnika.
     *
     * @param tree Drzewo kontraktu
     * @return Prefiks, np. "/api", albo pusty
     */
    static String prefix(final JsonNode tree) {
        return URI.create(
            tree.path("servers").path(0).path("url").asString("/").replaceAll("[{}]", "")
        ).getPath();
    }

    /**
     * Nazwa operacji z metody, prefiksu i szablonu ścieżki.
     *
     * @param verb Metoda HTTP w dowolnej wielkości liter
     * @param prefix Prefiks serwera, z końcowym ukośnikiem albo bez
     * @param path Szablon ścieżki
     * @return Nazwa "METODA prefiks/ścieżka" ze zmiennymi {@code {}}
     */
    static String name(final String verb, final String prefix, final String path) {
        return "%s %s%s".formatted(
            verb.toUpperCase(Locale.ROOT),
            prefix.replaceAll("/+$", ""),
            ContractOperation.VARIABLE.matcher(path).replaceAll("{}")
        );
    }
}
