/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.dostepnykampus.building.room.BuildingPlan;
import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanSource;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kroki szablonu „Import bierze oznaczenie, nazwę i opis wejścia z warstwy wejść PWr”:
 * wymyślony wpis warstwy wejść przechodzi przez odczyt usługi planów i zapis planu,
 * a wynik widać w karcie budynku.
 *
 * <p>Usługę planów zastępuje serwer HTTP na losowym porcie. Zapis planu i odczyt karty
 * idą w jednej transakcji, którą hak po scenariuszu wycofuje, więc plan budynku wraca
 * do stanu sprzed scenariusza.</p>
 *
 * <p>Wymagania: BR-27, US17.1, US2.6</p>
 *
 * @since 0.1.0
 */
public final class PwrEntranceSteps {

    /**
     * Numer warstwy wejść w usłudze zastępczej.
     */
    private static final int LAYER = 2;

    /**
     * Piętro wpisu i jedyne piętro zapisywanego planu.
     */
    private static final PlanFloor FLOOR = new PlanFloor("K1.A1.P0", "Parter", 0);

    /**
     * Pola wpisu słowami scenariusza.
     */
    private static final Pattern FIELDS = Pattern.compile(
        "Czy_dostos „(\\p{L}+)”, Szerokosc (\\d+), (?:Uwagi „(.+)”|puste Uwagi)"
    );

    /**
     * Oznaczenie z karty słowami scenariusza.
     */
    private static final Map<String, String> MARKINGS = Map.of(
        "ACCESSIBLE", "dostępne", "INACCESSIBLE", "niedostępne", "NO_DATA", "brak danych"
    );

    /**
     * Klient żądań HTTP do aplikacji.
     */
    private final MockMvc http;

    /**
     * Baza aplikacji.
     */
    private final JdbcClient jdbc;

    /**
     * Odczyt usługi planów.
     */
    private final PlanSource source;

    /**
     * Zapis planu budynku.
     */
    private final FloorPlans store;

    /**
     * Transakcje bazy aplikacji.
     */
    private final PlatformTransactionManager transactions;

    /**
     * Usługa zastępcza z warstwą wejść; null przed krokiem z wpisem.
     */
    private @Nullable HttpServer service;

    /**
     * Transakcja zapisu planu; null przed importem.
     */
    private @Nullable TransactionStatus transaction;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     * @param source Odczyt usługi planów
     * @param store Zapis planu budynku
     * @param transactions Transakcje bazy aplikacji
     */
    public PwrEntranceSteps(
        final MockMvc http, final JdbcClient jdbc, final PlanSource source,
        final FloorPlans store, final PlatformTransactionManager transactions
    ) {
        this.http = http;
        this.jdbc = jdbc;
        this.source = source;
        this.store = store;
        this.transactions = transactions;
    }

    /**
     * Usługa zastępcza podaje warstwę wejść z jednym wpisem wewnątrz konturu budynku.
     *
     * @param building Symbol budynku
     * @param fields Pola wpisu słowami scenariusza
     * @throws IOException Gdy serwer HTTP nie wystartuje
     */
    @Given("^w danych testowych warstwa wejść PWr ma przy (\\S+) wpis z polami (.+)$")
    public void layerEntry(final String building, final String fields) throws IOException {
        final Matcher found = PwrEntranceSteps.FIELDS.matcher(fields);
        MatcherAssert.assertThat(
            String.format("Scenariusz podaje nieznane pola wpisu: %s", fields),
            found.matches(), Matchers.is(true)
        );
        final String body = String.format(
            String.join(
                "",
                "{\"features\":[{\"geometry\":%s,\"properties\":{\"Pietro\":\"%s\",",
                "\"Czy_dostos\":\"%s\",\"Szerokosc\":%s,\"Uwagi\":%s}}]}"
            ),
            this.inside(building), PwrEntranceSteps.FLOOR.id(), found.group(1), found.group(2),
            PwrEntranceSteps.quoted(found.group(3))
        );
        final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
            String.format("/FeatureServer/%d/query", PwrEntranceSteps.LAYER),
            exchange -> PwrEntranceSteps.send(exchange, body)
        );
        server.start();
        this.service = server;
    }

    /**
     * Import czyta warstwę wejść z usługi zastępczej i zapisuje plan budynku A-1.
     */
    @When("^zakończy się import budynków$")
    public void imports() {
        final HttpServer server = this.service;
        if (server == null) {
            throw new IllegalStateException("Import przed krokiem z wpisem warstwy wejść");
        }
        final String url = String.format(
            "http://localhost:%d/FeatureServer", server.getAddress().getPort()
        );
        this.transaction = this.transactions.getTransaction(new DefaultTransactionDefinition());
        this.store.save(
            new BuildingPlan(
                "A-1", List.of(PwrEntranceSteps.FLOOR), List.of(),
                this.source.entrances(url, PwrEntranceSteps.LAYER)
            )
        );
    }

    /**
     * Karta budynku podaje wejście z warstwy albo wyjście ewakuacyjne.
     *
     * @param building Symbol budynku
     * @param expected Wynik słowami scenariusza
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("^(\\S+) ma (wejście .+|wyjście ewakuacyjne, a nie wejście)$")
    public void hasResult(final String building, final String expected) throws Exception {
        final String card = this.http.perform(
            MockMvcRequestBuilders.get(String.format("/buildings/%s", building))
        ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        MatcherAssert.assertThat(
            String.format("Karta budynku po imporcie: %s", card),
            PwrEntranceSteps.described(JsonMapper.builder().build().readTree(card)),
            Matchers.contains(expected)
        );
    }

    /**
     * Wycofuje zapis planu i zatrzymuje usługę zastępczą.
     */
    @After
    public void restore() {
        final TransactionStatus status = this.transaction;
        if (status != null) {
            this.transactions.rollback(status);
        }
        final HttpServer server = this.service;
        if (server != null) {
            server.stop(0);
        }
    }

    private String inside(final String building) {
        return this.jdbc.sql(
            "SELECT ST_AsGeoJSON(ST_PointOnSurface(outline)) FROM buildings WHERE symbol = :symbol"
        ).param("symbol", building).query(String.class).single();
    }

    private static List<String> described(final JsonNode card) {
        final List<String> result = new ArrayList<>(1);
        card.path("entrances").forEach(
            entrance -> {
                if (entrance.has("source")) {
                    result.add(PwrEntranceSteps.entrance(entrance));
                }
            }
        );
        result.addAll(
            Collections.nCopies(
                card.path("evacuationExits").size(), "wyjście ewakuacyjne, a nie wejście"
            )
        );
        return result;
    }

    private static String entrance(final JsonNode entrance) {
        final StringBuilder text = new StringBuilder(
            String.format(
                "wejście „%s”, „%s”", entrance.path("name").asString(""),
                PwrEntranceSteps.word(entrance.path("marking").asString(""))
            )
        );
        if (entrance.has("doorWidthCm")) {
            text.append(String.format(", drzwi %d cm", entrance.path("doorWidthCm").asInt()));
        } else {
            text.append(", bez szerokości drzwi");
        }
        if (entrance.has("approach")) {
            text.append(String.format(", opis „%s”", entrance.path("approach").asString("")));
        }
        return text.toString();
    }

    private static String word(final String marking) {
        return PwrEntranceSteps.MARKINGS.getOrDefault(marking, marking);
    }

    private static String quoted(final @Nullable String note) {
        final String result;
        if (note == null) {
            result = "null";
        } else {
            result = String.format("\"%s\"", note);
        }
        return result;
    }

    private static void send(final HttpExchange exchange, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/geo+json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
