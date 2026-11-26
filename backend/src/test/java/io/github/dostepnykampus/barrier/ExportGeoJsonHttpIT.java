/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.OpenApiContract;
import io.github.dostepnykampus.barrier.submission.content.BarrierDetails;
import io.github.dostepnykampus.barrier.submission.content.BarrierGroup;
import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.SubmissionContent;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Eksport bieżących przyjętych barier w formacie GeoJSON (WGS84) dla Koordynatora.
 *
 * <p>Wymagania: US13.1, US13.3, BR-6, BR-22, NFR-38, NFR-43</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class ExportGeoJsonHttpIT {

    @Test
    void enforcesCoordinatorRoleAndRejectsMissingOrOtherToken(
        @LocalServerPort final int port
    ) throws Exception {
        final OpenApiContract contract = new OpenApiContract();
        final HttpResponse<String> anon = ExportGeoJsonHttpIT.send(port, null);
        final HttpResponse<String> visitor = ExportGeoJsonHttpIT.send(port, "Bearer VISITOR");
        final HttpResponse<String> done = ExportGeoJsonHttpIT.send(port, "Bearer COORDINATOR");
        MatcherAssert.assertThat(
            "Kontrola dostępu albo schemat odpowiedzi GET /export.geojson jest błędna",
            List.of(
                anon.statusCode(), visitor.statusCode(), done.statusCode(),
                done.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElseThrow(),
                contract.violations("ExportFeatureCollection", done.body())
            ),
            Matchers.is(List.of(401, 403, 200, "application/geo+json;charset=UTF-8", List.of()))
        );
    }

    @Test
    void exportsOnlyCurrentAcceptedBarriersWithoutAuthorOrOutline(
        final WebApplicationContext context, @LocalServerPort final int port
    ) throws Exception {
        final SampleSubmissions samples = new SampleSubmissions(context);
        final SubmissionPlace inside = new SubmissionPlace(
            new SubmissionPoint(17.059_89, 51.109_16),
            null, false, "A-1", new SubmissionFloor(null, 0, true)
        );
        final UUID accepted = UUID.randomUUID();
        final SubmissionContent content = new SubmissionContent(
            SubmissionKind.PRECISE, inside,
            new BarrierDetails(
                BarrierGroup.ENTRANCE, new BarrierType(1), 3,
                "=Schody, \"próg\" i żółta poręcz", false
            ),
            null, null, null, List.of(UUID.randomUUID())
        );
        final List<UUID> created = new ArrayList<>(List.of(accepted));
        final String json;
        try {
            samples.add(accepted, "ACCEPTED", content);
            for (final String state : List.of("ARCHIVED", "IN_REVIEW", "SENT", "REJECTED")) {
                created.add(samples.add(state, content));
            }
            json = ExportGeoJsonHttpIT.send(port, "Bearer COORDINATOR").body();
        } finally {
            samples.remove(created);
        }
        final JsonNode features =
            context.getBean(ObjectMapper.class).readTree(json).path("features");
        final JsonNode one = ExportGeoJsonHttpIT.byId(features, accepted);
        final JsonNode props = one.path("properties");
        final String low = json.toLowerCase(Locale.ROOT);
        MatcherAssert.assertThat(
            "Eksport GeoJSON zawiera niewłaściwe zgłoszenia, dane autora, kontur albo zły układ",
            List.of(
                new OpenApiContract().violations("ExportFeatureCollection", json),
                features.size(), one.path("geometry").path("coordinates").toString(),
                props.path("barrierGroup").asString(), props.path("buildingSymbol").asString(),
                props.path("floor").asString(), props.path("description").asString(),
                low.contains("konto-testowe") || low.contains("author")
                    || low.contains("polygon") || low.contains("outline")
            ),
            Matchers.is(
                List.of(
                    List.of(), 1, "[17.05989,51.10916]", "ENTRANCE_AND_DOORS",
                    "A-1", "0 półpiętro", "=Schody, \"próg\" i żółta poręcz", false
                )
            )
        );
    }

    private static JsonNode byId(final JsonNode features, final UUID id) {
        return StreamSupport.stream(features.spliterator(), false).filter(
            item -> id.toString().equals(item.path("properties").path("submissionId").asString())
        ).findFirst().orElseThrow();
    }

    private static HttpResponse<String> send(
        final int port, final @Nullable String authorization
    ) throws Exception {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(
            URI.create("http://localhost:%d/export.geojson".formatted(port))
        ).timeout(Duration.ofSeconds(20));
        if (authorization != null) {
            builder.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(
                builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
        }
    }
}
