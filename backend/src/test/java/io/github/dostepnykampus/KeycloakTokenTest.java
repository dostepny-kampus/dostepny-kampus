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
 * (NFR-33, US14.1, ADR-0003): podpis i wystawcę realmu oraz role z
 * {@code realm_access.roles}.
 *
 * <p>Ma własny kontekst bez tożsamości testowej, bo sprawdza drogę używaną
 * w działającym systemie: adres realmu z ustawień, klucze pobrane z realmu
 * i odczyt ról z tokenu konta z {@code infra/keycloak/realm.json}.</p>
 *
 * @since 0.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(
    {
        PostgisTestConfiguration.class,
        RustfsTestConfiguration.class,
        KeycloakTestConfiguration.class
    }
)
final class KeycloakTokenTest {

    /**
     * Aplikacja mobilna jako klient realmu.
     */
    private static final String APP = "dostepny-kampus-android";

    @Test
    void nfrThirtyThreeRealTokenIsJudgedBySignatureAndRole(
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
                KeycloakTokenTest.token(
                    client, mapper, realm, KeycloakTokenTest.APP, "zbierajacy.test"
                ),
                KeycloakTokenTest.token(
                    client, mapper, realm, KeycloakTokenTest.APP, "odwiedzajacy.test"
                ),
                KeycloakTokenTest.token(
                    client, mapper, realm.replace("dostepny-kampus", "master"), "admin-cli",
                    KeycloakTestConfiguration.ADMIN
                )
            );
            final List<Integer> statuses = new ArrayList<>(tokens.size());
            for (final String token : tokens) {
                statuses.add(KeycloakTokenTest.edit(client, operation, token));
            }
            MatcherAssert.assertThat(
                "Token Zbierającego nie przechodzi, inna rola nie daje 403 albo obcy realm 401",
                statuses,
                Matchers.contains(
                    Matchers.not(Matchers.oneOf(401, 403)), Matchers.is(403), Matchers.is(401)
                )
            );
        }
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
        final HttpRequest.Builder request = HttpRequest.newBuilder(
            URI.create("%s/protocol/openid-connect/token".formatted(realm))
        );
        request.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE);
        request.POST(
            HttpRequest.BodyPublishers.ofString(
                "grant_type=password&client_id=%s&username=%s&password=%s".formatted(
                    app, user, KeycloakTokenTest.password(mapper, user)
                )
            )
        );
        return mapper.readTree(
            client.send(request.build(), HttpResponse.BodyHandlers.ofString()).body()
        ).path("access_token").asString();
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
