/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.dostepnykampus.building.room.PlanRoom;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * Klient usługi ArcGIS zamienia wymyśloną odpowiedź usługi na piętra i pomieszczenia
 * z konturem, numerem i funkcją.
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

    @Test
    void usSixteenReadsRoomOutlineNumberAndFunction() throws IOException {
        MatcherAssert.assertThat(
            "Pomieszczenie nie ma piętra, numeru, funkcji i konturu MultiPolygon",
            ArcgisPlansTest.rooms().get(0),
            Matchers.equalTo(
                new PlanRoom(
                    "T.X.P0", "101", "Sala",
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
    void nfrFortyRejectsErrorOfService() {
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> ArcgisPlansTest.read("{\"error\":{}}"),
            "Odpowiedź z błędem usługi została przyjęta"
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
            "{\"geometry\":{\"type\":\"Polygon\",\"coordinates\":", ArcgisPlansTest.SQUARE,
            "},\"properties\":{\"Pietro\":\"", floor, "\",\"NUMER\":", number,
            ",\"FUNKCJA\":", function, "}}"
        );
    }

    private static List<PlanRoom> read(final String body) throws IOException {
        final HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(
            "/FeatureServer/2/query", exchange -> ArcgisPlansTest.send(exchange, body)
        );
        server.start();
        try {
            return new ArcgisPlans(JsonMapper.builder().build()).rooms(
                String.format("http://localhost:%d/FeatureServer", server.getAddress().getPort()),
                2
            );
        } finally {
            server.stop(0);
        }
    }

    private static void send(final HttpExchange exchange, final String body) throws IOException {
        final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
