/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import io.github.dostepnykampus.identity.api.AccountId;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariusza „Weryfikator widzi rolę i pseudonim autora, a nie jego dane osobowe”.
 *
 * <p>Zbierający wysyła zgłoszenie z kontem {@code jan-kowalski} z własnego tokenu testowego,
 * z identyfikatorem zgłoszenia z {@link QueueSteps}; wzięcie i sprzątanie są w krokach kolejki,
 * a autora Weryfikator widzi w kolejce weryfikacji przy wziętym zgłoszeniu.</p>
 *
 * <p>Wymagania: BR-5</p>
 *
 * @since 0.1.0
 */
public final class AuthorSteps {

    /** Jedyne zdjęcie zgłoszenia. */
    private static final UUID PHOTO = UUID.fromString("c5d6e7f8-0002-4a8b-9c0d-1e2f3a4b5c6d");

    /** Konto autora w Usłudze logowania. */
    private static final String ACCOUNT = "jan-kowalski";

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Magazyn zdjęć zgłoszeń. */
    private final PhotoStore photos;

    /** Adres e-mail autora ze scenariusza. */
    private String email;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param photos Magazyn zdjęć zgłoszeń
     */
    public AuthorSteps(final MockMvc http, final PhotoStore photos) {
        this.http = http;
        this.photos = photos;
        this.email = "";
    }

    @Given("^zgłoszenie wysłał Zbierający Jan Kowalski z adresem e-mail (\\S+)$")
    void sentByCollector(final String address) throws Exception {
        this.email = address;
        MatcherAssert.assertThat(
            "Serwer nie przyjął zgłoszenia Zbierającego, nie dał 201",
            this.http.perform(
                MockMvcRequestBuilders.post("/submissions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer COLLECTOR.".concat(AuthorSteps.ACCOUNT))
                    .content(AuthorSteps.body())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(201)
        );
        this.photos.put(
            QueueSteps.ONE, AuthorSteps.PHOTO, "zdjęcie".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Then("widzi rolę autora „Zbierający” i pseudonim autora")
    void seesRoleAndPseudonym() throws Exception {
        MatcherAssert.assertThat(
            "Weryfikator nie widzi roli Zbierającego i pseudonimu autora",
            this.queued(),
            Matchers.allOf(
                Matchers.containsString("\"COLLECTOR\""),
                Matchers.containsString(
                    String.format(
                        "\"pseudonym\":\"%s\"", new AccountId(AuthorSteps.ACCOUNT).pseudonym()
                    )
                )
            )
        );
    }

    @Then("nie widzi imienia, nazwiska ani adresu e-mail autora")
    void seesNoPersonalData() throws Exception {
        MatcherAssert.assertThat(
            "Weryfikator widzi dane osobowe albo konto autora",
            this.queued(),
            Matchers.allOf(
                Matchers.not(Matchers.containsString("Jan")),
                Matchers.not(Matchers.containsString("Kowalski")),
                Matchers.not(Matchers.containsString(this.email)),
                Matchers.not(Matchers.containsString(AuthorSteps.ACCOUNT))
            )
        );
    }

    private String queued() throws Exception {
        return this.http.perform(
            MockMvcRequestBuilders.get("/review/queue").with(new AsVerifier())
        ).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String body() {
        return String.join(
            "",
            "{\"id\":\"", QueueSteps.ONE.toString(), "\",",
            "\"kind\":\"QUICK\",\"barrierGroup\":\"OTHER\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"photoIds\":[\"", AuthorSteps.PHOTO.toString(), "\"]}"
        );
    }
}
