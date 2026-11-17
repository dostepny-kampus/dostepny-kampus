/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serwer z domyślnym dekoderem ocenia token z prawdziwego Keycloaka
 * (ADR-0003): podpis, wystawcę realmu, odbiorcę serwera w {@code aud} oraz role z
 * {@code realm_access.roles}.
 *
 * <p>Ma własny kontekst bez tożsamości testowej, bo sprawdza drogę używaną
 * w działającym systemie: adres realmu z ustawień, klucze pobrane z realmu
 * i odczyt ról z tokenu konta z {@code infra/keycloak/realm.json}.</p>
 *
 * <p>Wymagania: NFR-33, US14.1</p>
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
final class KeycloakTokenIT {

    /**
     * Aplikacja mobilna jako klient realmu.
     */
    private static final String APP = "dostepny-kampus-android";

    @Test
    void judgesRealTokenBySignatureAndRole(
        final WebApplicationContext context
    ) throws Exception {
        final JsonMapper mapper = context.getBean(JsonMapper.class);
        final String realm = context.getEnvironment().getRequiredProperty(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri"
        );
        final String operation = "http://localhost:%s/submissions/%s".formatted(
            context.getEnvironment().getRequiredProperty("local.server.port"),
            "8d0c7a5e-1b7a-4c64-9f50-3c1f0d0e6a11"
        );
        try (HttpClient client = HttpClient.newHttpClient()) {
            final List<String> tokens = List.of(
                KeycloakTokenIT.token(
                    client, mapper, realm, KeycloakTokenIT.APP, "zbierajacy.test"
                ),
                KeycloakTokenIT.token(
                    client, mapper, realm, KeycloakTokenIT.APP, "odwiedzajacy.test"
                ),
                KeycloakTokenIT.token(
                    client, mapper, realm.replace("dostepny-kampus", "master"), "admin-cli",
                    KeycloakTestConfiguration.ADMIN
                ),
                KeycloakTokenIT.token(client, mapper, realm, "admin-cli", "zbierajacy.test")
            );
            final List<Integer> statuses = new ArrayList<>(tokens.size());
            for (final String token : tokens) {
                statuses.add(KeycloakTokenIT.edit(client, operation, token));
            }
            MatcherAssert.assertThat(
                "Zbierający nie przechodzi, inna rola nie ma 403 albo obcy token nie ma 401",
                statuses,
                Matchers.contains(
                    Matchers.not(Matchers.oneOf(401, 403)), Matchers.is(403), Matchers.is(401),
                    Matchers.is(401)
                )
            );
        }
    }

    @Test
    void rotatesRefreshTokenAndRejectsRetiredToken(
        final WebApplicationContext context
    ) throws Exception {
        final JsonMapper mapper = context.getBean(JsonMapper.class);
        final String realm = context.getEnvironment().getRequiredProperty(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri"
        );
        try (HttpClient client = HttpClient.newHttpClient()) {
            final String refresh = mapper.readTree(
                KeycloakTokenIT.login(
                    client, mapper, realm, KeycloakTokenIT.APP, "zbierajacy.test"
                ).body()
            ).path("refresh_token").asString();
            final String form = "grant_type=refresh_token&client_id=%s&refresh_token=%s"
                .formatted(KeycloakTokenIT.APP, refresh);
            final HttpResponse<String> renewed = KeycloakTokenIT.exchange(client, realm, form);
            final String next = mapper.readTree(renewed.body()).path("refresh_token").asString();
            final Rotation rotation = new Rotation(
                renewed.statusCode(), !refresh.isBlank() && !next.isBlank() && !next.equals(refresh),
                KeycloakTokenIT.exchange(client, realm, form).statusCode()
            );
            MatcherAssert.assertThat(
                "Pierwsze odświeżenie wymienia token, ponowne użycie wycofanego daje 400",
                rotation, Matchers.is(new Rotation(200, true, 400))
            );
        }
    }

    private record Rotation(int refreshStatus, boolean replaced, int retiredStatus) {
    }

    private static int edit(
        final HttpClient client, final String operation, final String token
    ) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create(operation))
                .header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(token))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build(),
            HttpResponse.BodyHandlers.discarding()
        ).statusCode();
    }

    private static String token(
        final HttpClient client, final JsonMapper mapper, final String realm,
        final String app, final String user
    ) throws Exception {
        return mapper.readTree(
            KeycloakTokenIT.login(client, mapper, realm, app, user).body()
        ).path("access_token").asString();
    }

    private static HttpResponse<String> login(
        final HttpClient client, final JsonMapper mapper, final String realm,
        final String app, final String user
    ) throws Exception {
        return KeycloakTokenIT.exchange(
            client, realm,
            "grant_type=password&client_id=%s&username=%s&password=%s".formatted(
                app, user, KeycloakTokenIT.password(mapper, user)
            )
        );
    }

    private static HttpResponse<String> exchange(
        final HttpClient client, final String realm, final String form
    ) throws Exception {
        return client.send(
            HttpRequest.newBuilder(URI.create("%s/protocol/openid-connect/token".formatted(realm)))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
    }

    private static String password(final JsonMapper mapper, final String user) {
        String found = KeycloakTestConfiguration.ADMIN;
        final JsonNode realm = mapper.readTree(Path.of("../infra/keycloak/realm.json").toFile());
        for (final JsonNode account : realm.path("users")) {
            if (user.equals(account.path("username").asString())) {
                found = account.path("credentials").path(0).path("value").asString();
            }
        }
        return found;
    }
}
