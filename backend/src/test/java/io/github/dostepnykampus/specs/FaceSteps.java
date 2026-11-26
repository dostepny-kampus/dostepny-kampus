/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Kroki scenariusza zasłonięcia twarzy: Weryfikator zastępuje zdjęcie zgłoszenia w weryfikacji
 * przez {@code PUT /submissions/{id}/photos/{photoId}} bez nowej wersji zgłoszenia.
 *
 * <p>Zgłoszenie ma identyfikator zgłoszenia z {@link VerificationSteps}, więc krok przyjęcia
 * i sprzątanie po scenariuszu są wspólne. Zdjęcie przed zasłonięciem jest jasne, a po nim
 * ciemne.</p>
 *
 * <p>Wymagania: NFR-25, BR-15</p>
 *
 * @since 0.1.0
 */
public final class FaceSteps {

    /** Zdjęcie zgłoszenia. */
    private static final UUID PHOTO = UUID.fromString("a9b8c7d6-0001-4e5f-8a6b-7c8d9e0f1a2b");

    /** Weryfikator ze scenariusza; token bez konta daje konto testowe. */
    private static final String VERIFIER = "Bearer VERIFIER";

    /** Klient żądań HTTP do aplikacji. */
    private final MockMvc http;

    /** Baza aplikacji. */
    private final JdbcClient jdbc;

    /**
     * Kroki na kontekście aplikacji.
     *
     * @param http Klient żądań HTTP do aplikacji
     * @param jdbc Baza aplikacji
     */
    public FaceSteps(final MockMvc http, final JdbcClient jdbc) {
        this.http = http;
        this.jdbc = jdbc;
    }

    /**
     * Autor wysłał zgłoszenie z jasnym zdjęciem, a pierwszy Weryfikator je wziął.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Given("^pierwszy Weryfikator wziął zgłoszenie z pomiaru precyzyjnego, na którego .+$")
    void claimedWithFace() throws Exception {
        final String author = "Bearer COLLECTOR.autor-zdjecia";
        MatcherAssert.assertThat(
            "Autor nie wysłał zgłoszenia ze zdjęciem albo Weryfikator go nie wziął",
            List.of(
                this.status(
                    MockMvcRequestBuilders.post("/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, author)
                        .content(FaceSteps.body())
                ),
                this.status(FaceSteps.photo(author, Color.WHITE)),
                this.status(
                    MockMvcRequestBuilders.post(
                        "/submissions/{id}/claim", VerificationSteps.SUBMISSION
                    ).header(HttpHeaders.AUTHORIZATION, FaceSteps.VERIFIER)
                )
            ),
            Matchers.contains(201, 204, 200)
        );
    }

    /**
     * Weryfikator zastępuje zdjęcie wersją z zasłoniętą twarzą.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @When("zasłoni twarz na zdjęciu")
    void coversFace() throws Exception {
        MatcherAssert.assertThat(
            "Serwer nie przyjął zdjęcia z zasłoniętą twarzą od Weryfikatora",
            this.status(FaceSteps.photo(FaceSteps.VERIFIER, Color.BLACK)),
            Matchers.equalTo(204)
        );
    }

    /**
     * Gość widzi przy zgłoszeniu przyjętym zdjęcie po zasłonięciu.
     *
     * @throws Exception Gdy MockMvc nie wykona żądania
     */
    @Then("zgłoszenie przyjęte pokazuje zdjęcie z zasłoniętą twarzą")
    void showsCovered() throws Exception {
        final BufferedImage image = ImageIO.read(
            new ByteArrayInputStream(
                this.http.perform(
                    MockMvcRequestBuilders.get(
                        "/submissions/{id}/photos/{photo}", VerificationSteps.SUBMISSION,
                        FaceSteps.PHOTO
                    )
                ).andReturn().getResponse().getContentAsByteArray()
            )
        );
        MatcherAssert.assertThat(
            "Zgłoszenie przyjęte pokazuje zdjęcie sprzed zasłonięcia twarzy",
            new Color(image.getRGB(image.getWidth() / 2, image.getHeight() / 2)).getRed(),
            Matchers.lessThan(64)
        );
    }

    /** Zgłoszenie ma wersję, którą autor już zna, więc jego edycja nie trafi na konflikt. */
    @Then("autor nie dostaje konfliktu wersji przy następnej synchronizacji")
    void sameVersion() {
        MatcherAssert.assertThat(
            "Zastąpienie zdjęcia przez Weryfikatora zmieniło wersję zgłoszenia",
            this.jdbc.sql("SELECT version FROM submissions WHERE id = :id")
                .param("id", VerificationSteps.SUBMISSION).query(Integer.class).single(),
            Matchers.equalTo(1)
        );
    }

    private int status(final RequestBuilder request) throws Exception {
        return this.http.perform(request).andReturn().getResponse().getStatus();
    }

    private static RequestBuilder photo(final String token, final Color color)
        throws IOException {
        final BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        final Graphics2D canvas = image.createGraphics();
        canvas.setColor(color);
        canvas.fillRect(0, 0, 64, 64);
        canvas.dispose();
        final ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpeg);
        return MockMvcRequestBuilders.put(
            "/submissions/{id}/photos/{photo}", VerificationSteps.SUBMISSION, FaceSteps.PHOTO
        ).contentType(MediaType.IMAGE_JPEG).header(HttpHeaders.AUTHORIZATION, token)
            .content(jpeg.toByteArray());
    }

    private static String body() {
        return String.join(
            "",
            "{\"id\":\"", VerificationSteps.SUBMISSION.toString(), "\",\"kind\":\"PRECISE\",",
            "\"location\":{\"type\":\"Point\",\"coordinates\":[17.06209,51.10785]},",
            "\"buildingSymbol\":\"C-13\",\"floor\":{\"number\":0},",
            "\"barrierGroup\":\"HORIZONTAL_CIRCULATION\",\"barrierType\":\"TB-13\",",
            "\"severity\":2,\"photoIds\":[\"", FaceSteps.PHOTO.toString(), "\"]}"
        );
    }
}
