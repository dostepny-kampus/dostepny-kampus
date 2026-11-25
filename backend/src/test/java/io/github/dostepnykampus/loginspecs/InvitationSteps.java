/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.loginspecs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Kroki scenariuszy o zaproszeniach: Koordynator zaprasza przez serwer, a pierwsze logowanie
 * zaproszonej osoby zastępuje ustawienie hasła kontem technicznym serwera w Keycloaku.
 *
 * <p>Wymagania: US12.1, US12.2, BR-1, BR-2</p>
 *
 * @since 0.1.0
 */
public final class InvitationSteps {

    /** Role kontraktu według nazw ze scenariuszy. */
    private static final Map<String, String> ROLES = Map.of(
        "Odwiedzający", "VISITOR", "Zbierający", "COLLECTOR",
        "Weryfikator", "VERIFIER", "Koordynator", "COORDINATOR"
    );

    /** Ustawienia kontekstu: port serwera, realm i konto techniczne. */
    private final Environment settings;

    /** Czytnik odpowiedzi JSON. */
    private final JsonMapper mapper;

    /** Adres e-mail zaproszonej osoby. */
    private String email;

    /** Token osoby, która wykonuje czynność. */
    private String token;

    /** Kod odpowiedzi serwera na ostatnie zaproszenie. */
    private int status;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param settings Ustawienia kontekstu
     * @param mapper Czytnik odpowiedzi JSON
     */
    public InvitationSteps(final Environment settings, final JsonMapper mapper) {
        this.settings = settings;
        this.mapper = mapper;
        this.email = "";
        this.token = "";
    }

    /**
     * Koordynator zaprasza adres z rolą.
     *
     * @param address Adres e-mail
     * @param role Rola ze scenariusza
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Given("^Koordynator wysłał zaproszenie na adres (\\S+) z rolą „([^”]+)”$")
    void invited(final String address, final String role) throws Exception {
        this.email = address;
        this.token = this.login("koordynator.test", this.password("koordynator.test"));
        this.invites(role);
    }

    /**
     * Konto z rolami Odwiedzającego i Zbierającego powstaje z zaproszenia Koordynatora.
     *
     * @param address Adres e-mail
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Given("^konto z adresem (\\S+) ma role Odwiedzający i Zbierający$")
    void existing(final String address) throws Exception {
        this.invited(address, "Zbierający");
    }

    /**
     * Koordynator zaprasza ten sam adres z kolejną rolą; serwer odpowiada 201.
     *
     * @param role Rola ze scenariusza
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @When("^Koordynator zaprosi ten adres z rolą „([^”]+)”$")
    void invites(final String role) throws Exception {
        this.status = this.invite(role);
        MatcherAssert.assertThat(
            "Serwer nie przyjął zaproszenia Koordynatora", this.status, Matchers.equalTo(201)
        );
    }

    /**
     * Osoba ustawia hasło, jak przy pierwszym logowaniu w przeglądarce, i loguje się.
     *
     * @throws Exception Gdy żądanie nie dojdzie do Keycloaka
     */
    @When("^(?:osoba z tym adresem zaloguje się pierwszy raz|Anna Nowak zaloguje się ponownie)$")
    void signsIn() throws Exception {
        final String secret = UUID.randomUUID().toString();
        final String account = this.account(this.email).path("id").asString();
        MatcherAssert.assertThat(
            "Hasło nie ustawione",
            this.admin(
                "/users/%s/reset-password".formatted(account),
                "{\"type\":\"password\",\"temporary\":false,\"value\":\"%s\"}".formatted(secret)
            ).statusCode(),
            Matchers.equalTo(204)
        );
        this.admin(
            "/users/%s".formatted(account), "{\"firstName\":\"Osoba\",\"lastName\":\"Zaproszona\"}"
        );
        this.token = this.login(this.email, secret);
    }

    /**
     * Profil z serwera ma rolę pracowniczą z zaproszenia.
     *
     * @param role Rola ze scenariusza
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Then("^w profilu widzi rolę „(Zbierający|Weryfikator|Koordynator)”$")
    void seesRole(final String role) throws Exception {
        MatcherAssert.assertThat(
            "Profil nie ma roli z zaproszenia", this.profile(),
            Matchers.hasItem(InvitationSteps.ROLES.get(role))
        );
    }

    /**
     * Profil z serwera ma dotychczasowe role i rolę z zaproszenia.
     *
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Then("^w profilu widzi role Odwiedzający, Zbierający i Weryfikator$")
    void seesRoles() throws Exception {
        MatcherAssert.assertThat(
            "Zaproszenie zmieniło dotychczasowe role konta", this.profile(),
            Matchers.containsInAnyOrder("VISITOR", "COLLECTOR", "VERIFIER")
        );
    }

    /**
     * Serwer nie odmawia osobie czynności ekranu.
     *
     * @param screen Nazwa ekranu ze scenariusza
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Then("^ma dostęp do ekranu „([^”]+)”$")
    void opens(final String screen) throws Exception {
        MatcherAssert.assertThat(
            "Serwer odmówił ekranu %s".formatted(screen), this.open(screen),
            Matchers.not(Matchers.oneOf(401, 403))
        );
    }

    /**
     * Serwer nie odmawia pomiaru precyzyjnego ani kolejki weryfikacji.
     *
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @Then("^może otworzyć pomiar precyzyjny i kolejkę weryfikacji$")
    void opensBoth() throws Exception {
        this.opens("pomiar precyzyjny");
        this.opens("kolejka weryfikacji");
    }

    /**
     * Weryfikator loguje się kontem testowym realmu.
     *
     * @throws Exception Gdy żądanie nie dojdzie do Keycloaka
     */
    @Given("^Weryfikator jest zalogowany z rolą Weryfikatora$")
    void verifier() throws Exception {
        this.token = this.login("weryfikator.test", this.password("weryfikator.test"));
    }

    /**
     * Weryfikator wysyła zaproszenie prosto do serwera.
     *
     * @param address Adres e-mail
     * @param role Rola ze scenariusza
     * @throws Exception Gdy żądanie nie dojdzie do serwera
     */
    @When("^spróbuje zaprosić adres (\\S+) z rolą „([^”]+)” z pominięciem aplikacji$")
    void tries(final String address, final String role) throws Exception {
        this.email = address;
        this.status = this.invite(role);
    }

    /** Serwer odmawia zaproszenia osobie bez roli Koordynatora. */
    @Then("^próba jest odrzucona$")
    void refused() {
        MatcherAssert.assertThat(
            "Serwer przyjął zaproszenie od Weryfikatora", this.status, Matchers.equalTo(403)
        );
    }

    /**
     * Keycloak nie ma konta z adresem albo konto nie ma roli Zbierającego.
     *
     * @param address Adres e-mail
     * @throws Exception Gdy żądanie nie dojdzie do Keycloaka
     */
    @Then("^konto z adresem (\\S+) nie dostaje roli Zbierającego$")
    void noRole(final String address) throws Exception {
        final JsonNode account = this.account(address);
        String roles = "[]";
        if (!account.isMissingNode()) {
            roles = this.get(
                "/users/%s/role-mappings/realm".formatted(account.path("id").asString())
            ).toString();
        }
        MatcherAssert.assertThat(
            "Konto dostało rolę z zaproszenia Weryfikatora", roles,
            Matchers.not(Matchers.containsString("COLLECTOR"))
        );
    }

    private int invite(final String role) throws Exception {
        return InvitationSteps.send(
            this.server("/invitations")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).POST(
                    HttpRequest.BodyPublishers.ofString(
                        "{\"email\":\"%s\",\"role\":\"%s\"}"
                            .formatted(this.email, InvitationSteps.ROLES.get(role))
                    )
                )
        ).statusCode();
    }

    private int open(final String screen) throws Exception {
        return InvitationSteps.send(
            switch (screen) {
                case "pomiar precyzyjny" -> this.server("/submissions")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).POST(
                        HttpRequest.BodyPublishers.ofString(
                            String.join(
                                "", "{\"id\":\"", UUID.randomUUID().toString(),
                                "\",\"kind\":\"PRECISE\",\"buildingSymbol\":\"C-13\",",
                                "\"floor\":{\"number\":0},",
                                "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",",
                                "\"barrierType\":\"TB-13\",\"severity\":2,\"photoIds\":[\"",
                                UUID.randomUUID().toString(),
                                "\"],\"location\":{\"type\":\"Point\",",
                                "\"coordinates\":[17.06209,51.10785]}}"
                            )
                        )
                    );
                case "kolejka weryfikacji" -> this.server("/review/queue");
                case "postęp inwentaryzacji" -> this.server("/progress");
                case null, default -> throw new IllegalArgumentException(
                    "Nieznany ekran %s".formatted(screen)
                );
            }
        ).statusCode();
    }

    private List<String> profile() throws Exception {
        return this.mapper.readTree(InvitationSteps.send(this.server("/me")).body())
            .path("roles").valueStream().map(JsonNode::asString).toList();
    }

    private HttpRequest.Builder server(final String path) {
        return HttpRequest.newBuilder(
            URI.create(
                "http://localhost:%s%s".formatted(
                    this.settings.getRequiredProperty("local.server.port"), path
                )
            )
        ).header(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(this.token));
    }

    private String login(final String user, final String secret) throws Exception {
        return this.issued(
            "%s/protocol/openid-connect/token".formatted(
                this.settings.getRequiredProperty(
                    "spring.security.oauth2.resourceserver.jwt.issuer-uri"
                )
            ),
            "grant_type=password&client_id=dostepny-kampus-android&username=%s&password=%s"
                .formatted(InvitationSteps.encoded(user), secret)
        );
    }

    private String issued(final String address, final String form) throws Exception {
        final HttpResponse<String> answer = InvitationSteps.send(
            HttpRequest.newBuilder(URI.create(address))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(form))
        );
        MatcherAssert.assertThat(
            "Keycloak nie wydał tokenu: %s".formatted(answer.body()), answer.statusCode(),
            Matchers.equalTo(200)
        );
        return this.mapper.readTree(answer.body()).path("access_token").asString();
    }

    private String password(final String user) {
        String found = "";
        final JsonNode realm = this.mapper.readTree(
            Path.of("../infra/keycloak/realm.json").toFile()
        );
        for (final JsonNode account : realm.path("users")) {
            if (user.equals(account.path("username").asString())) {
                found = account.path("credentials").path(0).path("value").asString();
            }
        }
        return found;
    }

    private JsonNode account(final String address) throws Exception {
        return this.get(
            "/users?email=%s&exact=true".formatted(InvitationSteps.encoded(address))
        ).path(0);
    }

    private JsonNode get(final String path) throws Exception {
        return this.mapper.readTree(InvitationSteps.send(this.admin(path)).body());
    }

    private HttpResponse<String> admin(final String path, final String json) throws Exception {
        return InvitationSteps.send(
            this.admin(path).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .PUT(HttpRequest.BodyPublishers.ofString(json))
        );
    }

    private HttpRequest.Builder admin(final String path) throws Exception {
        return HttpRequest.newBuilder(
            URI.create(this.settings.getRequiredProperty("dk.accounts.admin").concat(path))
        ).header(
            HttpHeaders.AUTHORIZATION,
            "Bearer %s".formatted(
                this.issued(
                    this.settings.getRequiredProperty("dk.accounts.token"),
                    "grant_type=client_credentials&client_id=%s&client_secret=%s".formatted(
                        this.settings.getRequiredProperty("dk.accounts.client"),
                        this.settings.getRequiredProperty("dk.accounts.secret")
                    )
                )
            )
        );
    }

    private static String encoded(final String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8);
    }

    private static HttpResponse<String> send(final HttpRequest.Builder request)
        throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
