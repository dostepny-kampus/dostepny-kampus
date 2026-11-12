/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Lista operacji kontraktu: prefiks serwera, nazwy zmiennych i klucze spoza metod (#829).
 *
 * @since 0.1.0
 */
final class ContractOperationTest {

    @Test
    void contractOperationsHavePrefixAnonymousVariablesAndOnlyVerbs() {
        MatcherAssert.assertThat(
            "Lista operacji zależy od nazw zmiennych albo bierze klucze, które nie są metodami",
            new OpenApiContract(
                () -> """
                    servers: [{url: "https://{host}/api/"}]
                    security: [{keycloak: []}]
                    paths:
                        /buildings/{symbol}/floors:
                            parameters: [{name: symbol, in: path}]
                            get: {security: []}
                            post: {}
                    """
            ).operations().stream().map(
                operation -> "%s %s".formatted(operation.name(), operation.security())
            ).toList(),
            Matchers.contains(
                "GET /api/buildings/{}/floors []",
                "POST /api/buildings/{}/floors [{\"keycloak\":[]}]"
            )
        );
    }

    @Test
    void contractOperationNameIgnoresVariableNamesAndTrailingSlash() {
        MatcherAssert.assertThat(
            "Nazwy zmiennych albo prefiks serwera wpływają na nazwę operacji",
            ContractOperation.name("get", "/api/", "/buildings/{id:[A-Z]+}/floors"),
            Matchers.equalTo(ContractOperation.name("GET", "/api", "/buildings/{symbol}/floors"))
        );
    }
}
