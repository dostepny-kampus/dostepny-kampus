/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account.storage;

import io.github.dostepnykampus.identity.account.Accounts;
import io.github.dostepnykampus.identity.account.Grant;
import io.github.dostepnykampus.identity.account.StaffRole;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

/**
 * Konta w Keycloak przez Admin REST API: token konta technicznego z {@code client_credentials},
 * wyszukanie konta po dokładnym adresie e-mail, założenie konta i mapowanie roli realmu.
 * Nowe konto trafia do grupy domyślnej realmu, czyli dostaje rolę Odwiedzającego.
 *
 * <p>Wymagania: US12.1, US12.2, BR-1, BR-2</p>
 *
 * @since 0.1.0
 */
@Component
@EnableConfigurationProperties(AccountSettings.class)
final class KeycloakAccounts implements Accounts {

    /** Limit czasu połączenia i odczytu odpowiedzi Usługi logowania. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    /** Ścieżka ról realmu konta pod adresem Admin REST API. */
    private static final String MAPPINGS = "/users/{id}/role-mappings/realm";

    /** Klient HTTP Usługi logowania. */
    private final RestClient client;

    /** Ustawienia konta technicznego. */
    private final AccountSettings settings;

    KeycloakAccounts(final AccountSettings settings) {
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(KeycloakAccounts.TIMEOUT);
        factory.setReadTimeout(KeycloakAccounts.TIMEOUT);
        this.client = RestClient.builder().requestFactory(factory).build();
        this.settings = settings;
    }

    @Override
    public Grant grant(final String email, final StaffRole role) {
        try {
            final String bearer = "Bearer %s".formatted(this.token());
            final Optional<String> found = this.find(bearer, email);
            final String account = found.orElseGet(() -> this.create(bearer, email));
            final Grant result;
            if (found.isPresent() && this.holds(bearer, account, role)) {
                result = Grant.UNCHANGED;
            } else {
                this.client.post()
                    .uri(this.settings.admin().concat(KeycloakAccounts.MAPPINGS), account)
                    .header(HttpHeaders.AUTHORIZATION, bearer)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(this.read(bearer, "/roles/{name}", role.name())))
                    .retrieve().toBodilessEntity();
                result = found.map(present -> Grant.ADDED).orElse(Grant.CREATED);
            }
            return result;
        } catch (final RestClientException ex) {
            throw new IllegalStateException(
                "Usługa logowania nie nadała roli %s: %s".formatted(role, ex.getMessage()), ex
            );
        }
    }

    private String token() {
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", this.settings.client());
        form.add("client_secret", this.settings.secret());
        return this.json(
            this.client.post().uri(this.settings.token())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(JsonNode.class)
        ).path("access_token").asString();
    }

    private Optional<String> find(final String bearer, final String email) {
        return this.read(bearer, "/users?email={email}&exact=true", email).valueStream()
            .map(user -> user.path("id").asString()).findFirst();
    }

    private String create(final String bearer, final String email) {
        this.client.post().uri(this.settings.admin().concat("/users"))
            .header(HttpHeaders.AUTHORIZATION, bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .body(
                Map.of(
                    "username", email, "email", email, "enabled", true,
                    "requiredActions", List.of("UPDATE_PASSWORD")
                )
            )
            .retrieve().toBodilessEntity();
        return this.find(bearer, email).orElseThrow(
            () -> new IllegalStateException("Usługa logowania nie pokazała nowego konta")
        );
    }

    private boolean holds(final String bearer, final String account, final StaffRole role) {
        return this.read(bearer, KeycloakAccounts.MAPPINGS, account).valueStream()
            .anyMatch(mapped -> role.name().equals(mapped.path("name").asString()));
    }

    private JsonNode read(final String bearer, final String path, final Object... values) {
        return this.json(
            this.client.get().uri(this.settings.admin().concat(path), values)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .retrieve().body(JsonNode.class)
        );
    }

    private JsonNode json(final @Nullable JsonNode body) {
        if (body == null) {
            throw new IllegalStateException("Usługa logowania odpowiedziała bez treści");
        }
        return body;
    }
}
