/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsCollector;
import io.github.dostepnykampus.IntegrationTest;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Testy BR-3 i US14.1: Zbierający nie zmienia cudzego zgłoszenia ani nie
 * dodaje do niego zdjęcia; serwer odpowiada 403 i niczego nie zapisuje.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class ForeignSubmissionHttpTest {

    /**
     * Zdjęcie z listy photoIds zgłoszenia próbnego.
     */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    @Test
    void brThreeRefusesEditOfForeignSubmission(final WebApplicationContext context)
        throws Exception {
        final UUID id = ForeignSubmissionHttpTest.foreign(context);
        MatcherAssert.assertThat(
            "Edycja cudzego zgłoszenia nie dała 403 albo zmieniła wersję",
            "%d %d".formatted(
                context.getBean(MockMvc.class).perform(
                    MockMvcRequestBuilders.put("/submissions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(new AsCollector())
                ).andReturn().getResponse().getStatus(),
                context.getBean(JdbcClient.class)
                    .sql("SELECT version FROM submissions WHERE id = :id")
                    .param("id", id).query(Integer.class).single()
            ),
            Matchers.equalTo("403 1")
        );
    }

    @Test
    void brThreeRefusesPhotoForForeignSubmission(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Zdjęcie do cudzego zgłoszenia nie dało 403",
            context.getBean(MockMvc.class).perform(
                MockMvcRequestBuilders.put(
                    "/submissions/{id}/photos/{photo}",
                    ForeignSubmissionHttpTest.foreign(context), ForeignSubmissionHttpTest.PHOTO
                ).contentType(MediaType.IMAGE_JPEG).content(new byte[] {1})
                    .with(new AsCollector())
            ).andReturn().getResponse().getStatus(),
            Matchers.equalTo(403)
        );
    }

    private static UUID foreign(final WebApplicationContext context) {
        final UUID id = new SampleSubmissions(context)
            .add("SENT", SampleSubmissions.quick(ForeignSubmissionHttpTest.PHOTO));
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = 'inne-konto' WHERE id = :id")
            .param("id", id).update();
        return id;
    }
}
