/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tools.jackson.databind.json.JsonMapper;

/**
 * Klient usługi ArcGIS zamienia wymyśloną odpowiedź usługi na piętra i pomieszczenia
 * z konturem, numerem i funkcją; przekroczenie limitu czasu kończy odczyt wyjątkiem.
 *
 * <p>Wymagania: US16, US18, NFR-40</p>
 *
 * @since 0.1.0
 */
final class ArcgisPlansTest {

    /**
     * Wielokąt z odpowiedzi usługi.
     */
    private static final String SQUARE = "[[[1,1],[1,2],[2,2],[2,1],[1,1]]]";

    /**
     * Adres zapytania warstwy pomieszczeń w lokalnym serwerze testowym.
     */
    private static final String QUERY = "/FeatureServer/2/query";

    @Test
    void usSixteenReadsRoomOutlineNumberAndFunction() throws IOException {
        MatcherAssert.assertThat(
            "Pomieszczenie nie ma piętra, numeru, funkcji i konturu MultiPolygon",
            ArcgisPlansTest.rooms().get(0),
            Matchers.equalTo(
                new PlanRoom(
                    "7", "T.X.P0", "101", "Sala",
                    String.format(
                        "{\"type\":\"MultiPolygon\",\"coordinates\":[%s]}", ArcgisPlansTest.SQUARE
                    )
                )
            )
        );
    }

    @Test
    void usEighteenGivesEmptyNumberAndKeepsFunctionOther() throws IOException {
        MatcherAssert.assertThat(
            "Brak numeru nie dał pustego numeru albo funkcja „Inne” nie została",
            ArcgisPlansTest.rooms().stream()
                .map(room -> String.join("/", room.number(), room.function())).toList(),
            Matchers.contains("101/Sala", "/Inne", "/Inne")
        );
    }

    @Test
    void usSixteenReadsFloorsWithIdLabelAndOrder() throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
            "/FeatureServer/1/query",
            exchange -> ArcgisPlansTest.send(
                exchange,
                String.join(
                    "",
                    "{\"features\":[",
                    "{\"geometry\":null,\"properties\":",
                    "{\"LEVEL_ID\":\"T.X.P0\",\"NAME\":\"Bud. X parter\",",
                    "\"LEVEL_NUMBER\":0}},",
                    "{\"geometry\":null,\"properties\":",
                    "{\"LEVEL_ID\":\"T.X.P00\",\"NAME\":\" Bud. X piwnica \",",
                    "\"LEVEL_NUMBER\":-1}},",
                    "{\"geometry\":null,\"properties\":{\"LEVEL_ID\":\" \",\"LEVEL_NUMBER\":2}}",
                    "]}"
                )
            )
        );
        server.start();
        try {
            MatcherAssert.assertThat(
                "Piętra nie mają identyfikatora, nazwy i numeru albo zostało piętro bez id",
                new ArcgisPlans(JsonMapper.builder().build()).floors(
                    String.format(
                        "http://localhost:%d/FeatureServer", server.getAddress().getPort()
                    ),
                    1
                ),
                Matchers.contains(
                    new PlanFloor("T.X.P0", "Bud. X parter", 0),
                    new PlanFloor("T.X.P00", "Bud. X piwnica", -1)
                )
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    void nfrFortyRejectsErrorOfService() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ArcgisPlansTest.read("{\"error\":{}}"),
            "Odpowiedź z błędem usługi została przyjęta"
        );
    }

    @Test
    @Timeout(3)
    void nfrFortyFailsWhenServiceTimesOut() {
        MatcherAssert.assertThat(
            "Komunikat przekroczenia czasu nie wskazuje limitu czasu i ścieżki usługi",
            ArcgisPlansTest.timeoutMessage(),
            Matchers.allOf(
                Matchers.containsString("limit czasu"),
                Matchers.containsString(ArcgisPlansTest.QUERY)
            )
        );
    }

    @Test
    void nfrFortyRejectsHttpErrorStatus() {
        MatcherAssert.assertThat(
            "Błąd HTTP usługi nie wskazał ścieżki usługi",
            Assertions.assertThrows(
                IllegalStateException.class,
                () -> ArcgisPlansTest.read(
                    new ArcgisPlans(JsonMapper.builder().build()),
                    exchange -> ArcgisPlansTest.status(exchange, 503)
                ),
                "Odpowiedź HTTP 503 usługi została przyjęta"
            ).getMessage(),
            Matchers.containsString(ArcgisPlansTest.QUERY)
        );
    }

    private static List<PlanRoom> rooms() throws IOException {
        return ArcgisPlansTest.read(
            String.format(
                "{\"features\":[%s]}",
                String.join(
                    ",",
                    ArcgisPlansTest.room("T.X.P0", "\"101\"", "\"Sala\""),
                    ArcgisPlansTest.room("T.X.P0", "\" \"", "null"),
                    ArcgisPlansTest.room("T.X.P00", "null", "\"Inne\""),
                    "{\"geometry\":null,\"properties\":{\"Pietro\":\"T.X.P0\"}}"
                )
            )
        );
    }

    private static String room(final String floor, final String number, final String function) {
        return String.join(
            "",
            "{\"id\":7,\"geometry\":{\"type\":\"Polygon\",\"coordinates\":",
            ArcgisPlansTest.SQUARE,
            "},\"properties\":{\"Pietro\":\"", floor, "\",\"NUMER\":", number,
            ",\"FUNKCJA\":", function, "}}"
        );
    }

    private static String timeoutMessage() {
        final CountDownLatch release = new CountDownLatch(1);
        return Assertions.assertThrows(
            IllegalStateException.class,
            () -> ArcgisPlansTest.read(
                new ArcgisPlans(Duration.ofMillis(100), JsonMapper.builder().build()),
                exchange -> ArcgisPlansTest.hang(exchange, release),
                release
            ),
            "Brak odpowiedzi usługi ArcGIS nie przerwał odczytu po limicie czasu"
        ).getMessage();
    }

    private static List<PlanRoom> read(final String body) throws IOException {
        return ArcgisPlansTest.read(
            new ArcgisPlans(JsonMapper.builder().build()),
            exchange -> ArcgisPlansTest.send(exchange, body)
        );
    }

    private static List<PlanRoom> read(
        final ArcgisPlans plans, final HttpHandler handler
    ) throws IOException {
        return ArcgisPlansTest.read(plans, handler, new CountDownLatch(0));
    }

    private static List<PlanRoom> read(
        final ArcgisPlans plans, final HttpHandler handler, final CountDownLatch release
    ) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(ArcgisPlansTest.QUERY, handler);
        server.start();
        try {
            return plans.rooms(
                String.format("http://localhost:%d/FeatureServer", server.getAddress().getPort()),
                2
            );
        } finally {
            release.countDown();
            server.stop(0);
        }
    }

    private static void send(final HttpExchange exchange, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        try (exchange) {
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        }
    }

    private static void status(final HttpExchange exchange, final int code) throws IOException {
        try (exchange) {
            exchange.sendResponseHeaders(code, -1);
        }
    }

    private static void hang(final HttpExchange exchange, final CountDownLatch release)
        throws IOException {
        try (exchange) {
            if (!release.await(10, TimeUnit.SECONDS)) {
                exchange.sendResponseHeaders(504, -1);
            }
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
