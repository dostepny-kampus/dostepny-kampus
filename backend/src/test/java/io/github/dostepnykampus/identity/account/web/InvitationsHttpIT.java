/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.web;

import io.github.dostepnykampus.KeycloakTestConfiguration;
import io.github.dostepnykampus.PostgisTestConfiguration;
import io.github.dostepnykampus.RustfsTestConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code POST /invitations} z prawdziwym Keycloakiem: Koordynator zakłada konto z rolą, dokłada
 * rolę istniejącemu kontu i dostaje 409 dla roli, którą konto już ma; Weryfikator niczego nie
 * zmienia.
 *
 * <p>Wymagania: US12.1, US12.2, BR-1, BR-2</p>
 *
 * @since 0.1.0
 */
@Tag("kontekst-springa")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(
    {
        PostgisTestConfiguration.class,
        RustfsTestConfiguration.class,
        KeycloakTestConfiguration.class
    }
)
final class InvitationsHttpIT {

    @Test
    void grantsRoleOnlyForCoordinator(final WebApplicationContext context) throws Exception {
        final Environment settings = context.getEnvironment();
        final JsonMapper mapper = context.getBean(JsonMapper.class);
        final String email = "zaproszenie-%s@example.test".formatted(UUID.randomUUID());
        try (HttpClient client = HttpClient.newHttpClient()) {
            final String coordinator = InvitationsHttpIT.token(
                client, mapper, settings, "koordynator.test"
            );
            final List<HttpResponse<String>> answers = List.of(
                InvitationsHttpIT.invite(
                    client, settings,
                    InvitationsHttpIT.token(client, mapper, settings, "weryfikator.test"),
                    email, "COLLECTOR"
                ),
                InvitationsHttpIT.invite(client, settings, coordinator, email, "COLLECTOR"),
                InvitationsHttpIT.invite(client, settings, coordinator, email, "VERIFIER"),
                InvitationsHttpIT.invite(client, settings, coordinator, email, "VERIFIER"),
                InvitationsHttpIT.invite(client, settings, coordinator, "bez-malpy", "VERIFIER")
            );
            MatcherAssert.assertThat(
                "Kody zaproszeń: Weryfikator, nowe konto, nowa rola, ta sama rola, zły adres",
                answers.stream().map(HttpResponse::statusCode).toList(),
                Matchers.contains(403, 201, 201, 409, 400)
            );
            MatcherAssert.assertThat(
                "Zaproszenie nie mówi, czy konto istniało, albo 409 bez rodzaju „rola już nadana”",
                List.of(
                    mapper.readTree(answers.get(1).body()).path("accountExisted").asBoolean(),
                    mapper.readTree(answers.get(2).body()).path("accountExisted").asBoolean(),
                    mapper.readTree(answers.get(3).body()).path("type").asString()
                ),
                Matchers.contains(false, true, "urn:dostepny-kampus:rola-juz-nadana")
            );
        }
    }

    private static HttpResponse<String> invite(
        final HttpClient client, final Environment settings, final String token,
        final String email, final String role
    ) throws Exception {
        return client.send(
            HttpRequest.newBuilder(
                URI.create(
                    "http://localhost:%s/invitations".formatted(
                        settings.getRequiredProperty("local.server.port")
                    )
                )
            )
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"%s\",\"role\":\"%s\"}".formatted(email, role)
                    )
                ).build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static String token(
        final HttpClient client, final JsonMapper mapper, final Environment settings,
        final String user
    ) throws Exception {
        final String password = "haslo-testowe-%s".formatted(user.replace(".test", ""));
        return mapper.readTree(
            client.send(
                HttpRequest.newBuilder(
                    URI.create(
                        settings.getRequiredProperty(
                            "spring.security.oauth2.resourceserver.jwt.issuer-uri"
                        ).concat("/protocol/openid-connect/token")
                    )
                )
                    .header(
                        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE
                    )
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            "grant_type=password&client_id=dostepny-kampus-android"
                                .concat("&username=%s&password=%s").formatted(user, password)
                        )
                    ).build(),
                HttpResponse.BodyHandlers.ofString()
            ).body()
        ).path("access_token").asString();
    }
}
