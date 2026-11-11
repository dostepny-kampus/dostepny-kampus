/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;

/**
 * Zabezpieczenia operacji według kontraktu {@code api/openapi.yaml} (NFR-33, US14.1).
 *
 * <p>Test czyta {@code security} operacji albo wpis globalny. Pusta lista albo wariant
 * {@code {}} to dostęp bez tokenu, kilka wpisów z rolami to którakolwiek z nich, a pusta lista
 * ról to dowolna zalogowana osoba. Operacja zamknięta odpowiada 401 bez tokenu i 403 dla roli
 * spoza kontraktu. Operacje bez handlera też są sprawdzane: serwer zna ich reguły.</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class SecurityContractTest {

    /**
     * Role z kontraktu.
     */
    private static final List<String> ROLES = List.of(
        "VISITOR", "COLLECTOR", "VERIFIER", "COORDINATOR"
    );

    /**
     * Wywołanie bez nagłówka z tokenem.
     */
    private static final String ANONYMOUS = "bez tokenu";

    /**
     * Kontrakt próbny z każdym rodzajem wpisu {@code security}.
     */
    private static final String DRAFT = """
        servers: [{url: /api}]
        security: [{keycloak: []}]
        paths:
            /role: {get: {security: [{keycloak: [VERIFIER]}]}}
            /two/{id}: {delete: {security: [{keycloak: [COLLECTOR]}, {keycloak: [COORDINATOR]}]}}
            /open: {get: {security: []}}
            /both: {get: {security: [{}, {keycloak: []}]}}
            /global: {post: {}}
        """;

    @Test
    void nfrThirtyThreeEveryOperationFollowsContractSecurity(
        final WebApplicationContext context
    ) throws Exception {
        final String base = "http://localhost:%s".formatted(
            context.getEnvironment().getProperty("local.server.port")
        );
        try (HttpClient client = HttpClient.newHttpClient()) {
            MatcherAssert.assertThat(
                "Serwer wpuszcza albo odrzuca wywołania inaczej, niż opisuje kontrakt",
                SecurityContractTest.problems(
                    SecurityContractTest.rules(
                        Files.readString(Path.of("..", "api", "openapi.yaml")), Set.of()
                    ),
                    (operation, who) -> SecurityContractTest.status(client, base, operation, who)
                ),
                Matchers.empty()
            );
        }
    }

    @Test
    void nfrThirtyThreeRejectionIsContractProblem(
        final WebApplicationContext context
    ) throws Exception {
        final String base = "http://localhost:%s".formatted(
            context.getEnvironment().getProperty("local.server.port")
        );
        try (HttpClient client = HttpClient.newHttpClient()) {
            final List<String> found = new ArrayList<>(0);
            final List<String> callers = List.of(
                SecurityContractTest.ANONYMOUS, "zly-token", "VISITOR"
            );
            for (final String who : callers) {
                final HttpResponse<String> reply = SecurityContractTest.reply(
                    client, base, "GET /review/queue", who
                );
                found.add(
                    "%d %s %s".formatted(
                        reply.statusCode(),
                        reply.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(""),
                        new OpenApiContract().violations("Problem", reply.body())
                    )
                );
            }
            MatcherAssert.assertThat(
                "Odmowa nie jest odpowiedzią Problem z kontraktu albo zły token nie daje 401",
                found,
                Matchers.contains(
                    "401 application/problem+json []", "401 application/problem+json []",
                    "403 application/problem+json []"
                )
            );
        }
    }

    @Test
    void nfrThirtyThreeOpenServerIsReportedForEveryClosedOperation() throws Exception {
        MatcherAssert.assertThat(
            "Serwer otwarty dla każdego przeszedł kontrolę zabezpieczeń",
            SecurityContractTest.problems(
                SecurityContractTest.rules(SecurityContractTest.DRAFT, Set.of()),
                (operation, who) -> 200
            ),
            Matchers.contains(
                "DELETE /api/two/{}, bez tokenu: status 200, kontrakt wymaga 401",
                "DELETE /api/two/{}, VISITOR: status 200, kontrakt wymaga 403",
                "DELETE /api/two/{}, VERIFIER: status 200, kontrakt wymaga 403",
                "GET /api/role, bez tokenu: status 200, kontrakt wymaga 401",
                "GET /api/role, VISITOR: status 200, kontrakt wymaga 403",
                "GET /api/role, COLLECTOR: status 200, kontrakt wymaga 403",
                "GET /api/role, COORDINATOR: status 200, kontrakt wymaga 403",
                "POST /api/global, bez tokenu: status 200, kontrakt wymaga 401"
            )
        );
    }

    @Test
    void nfrThirtyThreeServerMatchingContractPasses() throws Exception {
        final Map<String, Set<String>> allowed = Map.of(
            "GET /api/role", Set.of("VERIFIER"),
            "DELETE /api/two/{}", Set.of("COLLECTOR", "COORDINATOR"),
            "POST /api/global", Set.copyOf(SecurityContractTest.ROLES)
        );
        MatcherAssert.assertThat(
            "Serwer zgodny z kontraktem nie przeszedł kontroli zabezpieczeń",
            SecurityContractTest.problems(
                SecurityContractTest.rules(SecurityContractTest.DRAFT, Set.of()),
                (operation, who) -> {
                    int status = 404;
                    if (allowed.containsKey(operation) && !allowed.get(operation).contains(who)) {
                        status = 403;
                        if (SecurityContractTest.ANONYMOUS.equals(who)) {
                            status = 401;
                        }
                    }
                    return status;
                }
            ),
            Matchers.empty()
        );
    }

    @Test
    void nfrThirtyThreeClosedPublicOperationIsReported() throws Exception {
        MatcherAssert.assertThat(
            "Operacja publiczna zamknięta przez serwer przeszła kontrolę",
            SecurityContractTest.problems(
                SecurityContractTest.rules(
                    SecurityContractTest.DRAFT,
                    Set.of("GET /api/role", "DELETE /api/two/{}", "POST /api/global")
                ),
                (operation, who) -> {
                    int status = 200;
                    if (SecurityContractTest.ANONYMOUS.equals(who)) {
                        status = 401;
                    }
                    return status;
                }
            ),
            Matchers.contains(
                "GET /api/both, bez tokenu: status 401, kontrakt wymaga innego niż 401 i 403",
                "GET /api/open, bez tokenu: status 401, kontrakt wymaga innego niż 401 i 403"
            )
        );
    }

    private static List<String> problems(
        final Map<String, JsonNode> rules, final Probe probe
    ) throws Exception {
        final List<String> callers = new ArrayList<>(List.of(SecurityContractTest.ANONYMOUS));
        callers.addAll(SecurityContractTest.ROLES);
        final List<String> found = new ArrayList<>(0);
        for (final Map.Entry<String, JsonNode> rule : rules.entrySet()) {
            for (final String who : callers) {
                final int wanted = SecurityContractTest.wanted(rule.getValue(), who);
                final int status = probe.status(rule.getKey(), who);
                String label = "innego niż 401 i 403";
                boolean fine = status != 401 && status != 403;
                if (wanted != 0) {
                    label = String.valueOf(wanted);
                    fine = wanted == status;
                }
                if (!fine) {
                    found.add(
                        "%s, %s: status %d, kontrakt wymaga %s"
                            .formatted(rule.getKey(), who, status, label)
                    );
                }
            }
        }
        return found;
    }

    private static int wanted(final JsonNode security, final String who) {
        boolean open = security.isEmpty();
        final Set<String> roles = new TreeSet<>();
        for (final JsonNode variant : security) {
            open = open || variant.isEmpty();
            variant.path("keycloak").forEach(role -> roles.add(role.asString()));
        }
        int status = 0;
        if (!open && SecurityContractTest.ANONYMOUS.equals(who)) {
            status = 401;
        } else if (!open && !roles.isEmpty() && !roles.contains(who)) {
            status = 403;
        }
        return status;
    }

    private static Map<String, JsonNode> rules(
        final String text, final Set<String> skipped
    ) {
        final Map<String, JsonNode> rules = new TreeMap<>();
        for (final ContractOperation operation : new OpenApiContract(() -> text).operations()) {
            if (!skipped.contains(operation.name())) {
                rules.put(operation.name(), operation.security());
            }
        }
        return rules;
    }

    private static int status(
        final HttpClient client, final String base, final String operation, final String who
    ) throws Exception {
        return SecurityContractTest.reply(client, base, operation, who).statusCode();
    }

    private static HttpResponse<String> reply(
        final HttpClient client, final String base, final String operation, final String who
    ) throws Exception {
        final String[] parts = operation.split(" ", 2);
        HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.noBody();
        if (Set.of("POST", "PUT", "PATCH").contains(parts[0])) {
            body = HttpRequest.BodyPublishers.ofString("{}");
        }
        final HttpRequest.Builder request = HttpRequest.newBuilder(
            URI.create(base.concat(parts[1].replace("{}", "00000000-0000-0000-0000-000000000000")))
        ).method(parts[0], body).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (!SecurityContractTest.ANONYMOUS.equals(who)) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer ".concat(who));
        }
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Wywołanie operacji przez osobę bez tokenu albo z rolą.
     *
     * @since 0.1.0
     */
    @FunctionalInterface
    private interface Probe {

        /**
         * Status odpowiedzi.
         *
         * @param operation Operacja "METODA ścieżka"
         * @param who Rola z kontraktu albo wywołanie bez tokenu
         * @return Status HTTP
         * @throws Exception Gdy wywołanie się nie uda
         */
        int status(String operation, String who) throws Exception;
    }
}
