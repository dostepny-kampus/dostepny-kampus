/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Testy BR-7, US2.2 i NFR-26: zdjęcie z listy photoIds trafia do magazynu
 * bez metadanych, a odczyt zdjęcia działa dla zgłoszenia przyjętego.
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import({PostgisTestConfiguration.class, RustfsTestConfiguration.class})
@Transactional
final class SubmissionPhotosHttpTest {

    /**
     * Zdjęcie z listy photoIds zgłoszenia testowego.
     */
    private static final UUID PHOTO = UUID.fromString(
        "5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d"
    );

    @Test
    void brSevenSavesListedPhoto(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Zdjęcie z listy photoIds nie daje 204",
            SubmissionPhotosHttpTest.put(
                context,
                SubmissionPhotosHttpTest.add(context, "SENT"),
                SubmissionPhotosHttpTest.PHOTO
            ).getStatus(),
            Matchers.equalTo(204)
        );
    }

    @Test
    void brSevenRejectsUnlistedPhoto(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Zdjęcie spoza listy photoIds nie daje 404",
            SubmissionPhotosHttpTest.put(
                context, SubmissionPhotosHttpTest.add(context, "SENT"), UUID.randomUUID()
            ).getStatus(),
            Matchers.equalTo(404)
        );
    }

    @Test
    void brFifteenLocksAcceptedPhoto(final WebApplicationContext context) throws Exception {
        MatcherAssert.assertThat(
            "Zmiana zdjęcia zgłoszenia przyjętego bez logowania nie daje 403",
            SubmissionPhotosHttpTest.put(
                context,
                SubmissionPhotosHttpTest.add(context, "ACCEPTED"),
                SubmissionPhotosHttpTest.PHOTO
            ).getStatus(),
            Matchers.equalTo(403)
        );
    }

    @Test
    void nfrTwentySixServesPhotoWithoutExif(final WebApplicationContext context)
        throws Exception {
        final UUID id = SubmissionPhotosHttpTest.add(context, "SENT");
        SubmissionPhotosHttpTest.put(context, id, SubmissionPhotosHttpTest.PHOTO);
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET status = 'ACCEPTED' WHERE id = :id")
            .param("id", id)
            .update();
        MatcherAssert.assertThat(
            "Zdjęcie zgłoszenia przyjętego ma EXIF z miejscem",
            SubmissionPhotosHttpTest.get(context, id)
                .getContentAsString(StandardCharsets.ISO_8859_1),
            Matchers.allOf(
                Matchers.startsWith("ÿØ"), Matchers.not(Matchers.containsString("GPS"))
            )
        );
    }

    @Test
    void brSixHidesPhotoOfSentSubmission(final WebApplicationContext context) throws Exception {
        final UUID id = SubmissionPhotosHttpTest.add(context, "SENT");
        SubmissionPhotosHttpTest.put(context, id, SubmissionPhotosHttpTest.PHOTO);
        MatcherAssert.assertThat(
            "Zdjęcie zgłoszenia wysłanego jest widoczne bez logowania",
            SubmissionPhotosHttpTest.get(context, id).getStatus(),
            Matchers.equalTo(404)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"GIF89a", "ÿØÿá", "ÿØxÿÙ"})
    void nfrTwentySixRejectsInvalidJpeg(
        final String content, final WebApplicationContext context
    ) throws Exception {
        MatcherAssert.assertThat(
            "Niepoprawny nagłówek JPEG nie daje 400",
            SubmissionPhotosHttpTest.mvc(context).perform(
                MockMvcRequestBuilders.put(
                    "/submissions/{id}/photos/{photo}",
                    SubmissionPhotosHttpTest.add(context, "SENT"), SubmissionPhotosHttpTest.PHOTO
                ).contentType(MediaType.IMAGE_JPEG)
                    .content(content.getBytes(StandardCharsets.ISO_8859_1))
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(400)
        );
    }

    private static UUID add(final WebApplicationContext context, final String status) {
        final UUID id = UUID.randomUUID();
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO submissions (id, status, kind, location, content)
            VALUES (:id, :status, 'QUICK', ST_SetSRID(ST_MakePoint(17.0, 51.0), 4326),
                CAST(:content AS JSONB))
            """
        ).param("id", id)
            .param("status", status)
            .param(
                "content",
                String.join(
                    "",
                    "{\"kind\":\"QUICK\",\"location\":{\"type\":\"Point\",",
                    "\"coordinates\":[17.0,51.0]},\"barrierGroup\":\"OTHER\",",
                    "\"photoIds\":[\"%s\"]}"
                ).formatted(SubmissionPhotosHttpTest.PHOTO)
            )
            .update();
        return id;
    }

    private static MockHttpServletResponse put(
        final WebApplicationContext context, final UUID id, final UUID photo
    ) throws Exception {
        return SubmissionPhotosHttpTest.mvc(context).perform(
            MockMvcRequestBuilders.put("/submissions/{id}/photos/{photo}", id, photo)
                .contentType(MediaType.IMAGE_JPEG)
                .content(JpegCleanerTest.photo())
        ).andReturn().getResponse();
    }

    private static MockHttpServletResponse get(
        final WebApplicationContext context, final UUID id
    ) throws Exception {
        return SubmissionPhotosHttpTest.mvc(context).perform(
            MockMvcRequestBuilders.get(
                "/submissions/{id}/photos/{photo}", id, SubmissionPhotosHttpTest.PHOTO
            )
        ).andReturn().getResponse();
    }

    private static MockMvc mvc(final WebApplicationContext context) {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }
}
