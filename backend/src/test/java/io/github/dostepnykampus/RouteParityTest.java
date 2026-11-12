/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Parytet tras serwera i kontraktu {@code api/openapi.yaml} (decyzja 122).
 *
 * <p>Operacja to para "METODA ścieżka" z {@link ContractOperation}; nazwy
 * zmiennych w ścieżce nie mają znaczenia, a prefiks z {@code servers} jest doliczany.
 * Każda operacja kontraktu ma handler w kontekście Springa albo stoi
 * na zamkniętej liście operacji jeszcze niezrobionych, z powodem. Każdy
 * handler aplikacji ma operację w kontrakcie. Operacja z listy, która
 * ma już handler, jest błędem, więc lista tylko się kurczy. Handlery
 * Springa, Actuatora i klasy testowe z {@code @TestComponent} są poza zakresem.</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class RouteParityTest {

    /**
     * Operacje kontraktu bez handlera i powód każdej; lista tylko się kurczy.
     */
    static final Map<String, String> PENDING = Map.ofEntries(
        Map.entry("GET /buildings/{}/floors", "piętra budynku: moduł budynków"),
        Map.entry("GET /buildings/{}/floors/{}/rooms", "sale piętra: moduł budynków"),
        Map.entry("GET /rooms", "wyszukiwanie sal: moduł budynków"),
        Map.entry("GET /review/queue", "kolejka Weryfikatora: moduł weryfikacji"),
        Map.entry("POST /submissions/{}/claim", "przejęcie zgłoszenia: moduł weryfikacji"),
        Map.entry("POST /submissions/{}/review", "decyzja Weryfikatora: moduł weryfikacji"),
        Map.entry("POST /sync/push", "wysyłka zmian z aplikacji: synchronizacja"),
        Map.entry("GET /sync/pull", "pobranie zmian do aplikacji: synchronizacja"),
        Map.entry("DELETE /inspection-markers/{}", "znaczniki inspekcji: moduł inspekcji"),
        Map.entry("GET /progress", "postęp inspekcji: moduł inspekcji"),
        Map.entry("GET /export.geojson", "eksport GeoJSON: moduł eksportu"),
        Map.entry("GET /export.csv", "eksport CSV: moduł eksportu"),
        Map.entry("POST /invitations", "zaproszenia: moduł tożsamości")
    );

    @Test
    void routeParityEveryOperationHasHandlerAndEveryHandlerHasOperation(
        final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Trasy serwera i kontrakt openapi.yaml rozjechały się",
            RouteParityTest.problems(
                RouteParityTest.operations(),
                RouteParityTest.handlers(context),
                RouteParityTest.PENDING
            ),
            Matchers.empty()
        );
    }

    @Test
    void routeParityNewRouteWithoutOperationIsReported() {
        MatcherAssert.assertThat(
            "Nowa trasa bez operacji w kontrakcie przeszła",
            RouteParityTest.problems(
                Set.of("GET /a"), Set.of("GET /a", "GET /b/{}"), Map.of()
            ),
            Matchers.contains("handler bez operacji w kontrakcie: GET /b/{}")
        );
    }

    @Test
    void routeParityOperationWithoutHandlerOutsideListIsReported() {
        MatcherAssert.assertThat(
            "Operacja kontraktu bez handlera i spoza listy przeszła",
            RouteParityTest.problems(
                Set.of("GET /a", "GET /b"), Set.of("GET /a"), Map.of()
            ),
            Matchers.contains("operacja kontraktu bez handlera i spoza listy: GET /b")
        );
    }

    @Test
    void routeParityStaleListEntryIsReported() {
        MatcherAssert.assertThat(
            "Wpis listy dla operacji, która ma już handler, przeszedł",
            RouteParityTest.problems(
                Set.of("GET /a"), Set.of("GET /a"), Map.of("GET /a", "powód")
            ),
            Matchers.contains("wpis listy ma już handler, usuń go z listy: GET /a")
        );
    }

    @Test
    void routeParityListEntryWithoutOperationIsReported() {
        MatcherAssert.assertThat(
            "Wpis listy dla operacji, której nie ma w kontrakcie, przeszedł",
            RouteParityTest.problems(
                Set.of("GET /a"), Set.of("GET /a"), Map.of("GET /gone", "powód")
            ),
            Matchers.contains("wpis listy bez operacji w kontrakcie: GET /gone")
        );
    }

    @Test
    void routeParityDifferentVerbOnSamePathIsReportedOnBothSides() {
        MatcherAssert.assertThat(
            "Ta sama ścieżka z inną metodą HTTP uznana za parytet",
            RouteParityTest.problems(
                Set.of("POST /a"), Set.of("GET /a"), Map.of()
            ),
            Matchers.contains(
                "handler bez operacji w kontrakcie: GET /a",
                "operacja kontraktu bez handlera i spoza listy: POST /a"
            )
        );
    }

    @Test
    void routeParityListEntryCoversOnlyItsOwnPair() {
        MatcherAssert.assertThat(
            "Wpis listy objął inną operację tego samego prefiksu",
            RouteParityTest.problems(
                Set.of("GET /a", "GET /sync/pull", "POST /sync/push"),
                Set.of("GET /a"),
                Map.of("POST /sync", "cały prefiks", "GET /sync/pull", "powód")
            ),
            Matchers.contains(
                "operacja kontraktu bez handlera i spoza listy: POST /sync/push",
                "wpis listy bez operacji w kontrakcie: POST /sync"
            )
        );
    }

    @Test
    void routeParityEmptyHandlersAreReported() {
        MatcherAssert.assertThat(
            "Pusty zbiór handlerów przeszedł",
            RouteParityTest.problems(Set.of(), Set.of(), Map.of()),
            Matchers.hasItems("brak handlerów aplikacji", "brak operacji w kontrakcie")
        );
    }

    private static List<String> problems(
        final Set<String> spec, final Set<String> handlers, final Map<String, String> pending
    ) {
        final List<String> found = new ArrayList<>(0);
        if (handlers.isEmpty()) {
            found.add("brak handlerów aplikacji");
        }
        if (spec.isEmpty()) {
            found.add("brak operacji w kontrakcie");
        }
        new TreeSet<>(handlers).stream().filter(item -> !spec.contains(item))
            .map("handler bez operacji w kontrakcie: "::concat).forEach(found::add);
        new TreeSet<>(spec).stream()
            .filter(item -> !handlers.contains(item) && !pending.containsKey(item))
            .map("operacja kontraktu bez handlera i spoza listy: "::concat)
            .forEach(found::add);
        new TreeSet<>(pending.keySet()).forEach(
            item -> {
                if (spec.contains(item)) {
                    if (handlers.contains(item)) {
                        found.add("wpis listy ma już handler, usuń go z listy: ".concat(item));
                    }
                } else {
                    found.add("wpis listy bez operacji w kontrakcie: ".concat(item));
                }
            }
        );
        return found;
    }

    private static Set<String> handlers(final WebApplicationContext context) {
        return context.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class)
            .getHandlerMethods().entrySet().stream()
            .filter(entry -> RouteParityTest.ours(entry.getValue()))
            .flatMap(entry -> RouteParityTest.routes(entry.getKey()).stream())
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean ours(final HandlerMethod handler) {
        return handler.getBeanType().getName().startsWith("io.github.dostepnykampus.")
            && !handler.getBeanType().isAnnotationPresent(TestComponent.class);
    }

    private static List<String> routes(final RequestMappingInfo info) {
        final Set<String> verbs = info.getMethodsCondition().getMethods().stream()
            .map(Enum::name).collect(Collectors.toSet());
        if (verbs.isEmpty()) {
            verbs.add("ANY");
        }
        return verbs.stream().flatMap(
            verb -> info.getPatternValues().stream()
                .map(pattern -> ContractOperation.name(verb, "", pattern))
        ).toList();
    }

    private static Set<String> operations() {
        return new OpenApiContract().operations().stream().map(ContractOperation::name)
            .collect(Collectors.toCollection(TreeSet::new));
    }
}
