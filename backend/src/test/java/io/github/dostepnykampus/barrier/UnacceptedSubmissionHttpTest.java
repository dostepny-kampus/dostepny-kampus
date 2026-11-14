/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.AsCollector;
import io.github.dostepnykampus.AsVerifier;
import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.barrier.photo.PhotoStore;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Zgłoszenie nieprzyjęte i jego zdjęcie widzi po logowaniu autor
 * i Weryfikator; inna osoba i osoba bez tokenu dostają 404 jak przy braku.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class UnacceptedSubmissionHttpTest {

    /**
     * Zdjęcie z listy photoIds zgłoszenia próbnego.
     */
    private static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Kody dla autora, Weryfikatora, innej osoby i osoby bez tokenu.
     */
    private static final List<Integer> CODES = List.of(200, 200, 404, 404);

    @Test
    void brFiveShowsUnacceptedToAuthorAndVerifier(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Zgłoszenie nieprzyjęte widzi nie tylko autor i Weryfikator",
            UnacceptedSubmissionHttpTest.codes(context, "/submissions/{id}"),
            Matchers.equalTo(UnacceptedSubmissionHttpTest.CODES)
        );
    }

    @Test
    void brFiveShowsUnacceptedPhotoToAuthorAndVerifier(final WebApplicationContext context)
        throws Exception {
        MatcherAssert.assertThat(
            "Zdjęcie zgłoszenia nieprzyjętego widzi nie tylko autor i Weryfikator",
            UnacceptedSubmissionHttpTest.codes(
                context,
                "/submissions/{id}/photos/".concat(UnacceptedSubmissionHttpTest.PHOTO.toString())
            ),
            Matchers.equalTo(UnacceptedSubmissionHttpTest.CODES)
        );
    }

    private static List<Integer> codes(final WebApplicationContext context, final String path)
        throws Exception {
        final UUID own = UnacceptedSubmissionHttpTest.sent(context);
        final UUID foreign = UnacceptedSubmissionHttpTest.sent(context);
        context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET author = 'inne-konto' WHERE id = :id")
            .param("id", foreign).update();
        return List.of(
            UnacceptedSubmissionHttpTest.code(
                context, MockMvcRequestBuilders.get(path, own).with(new AsCollector())
            ),
            UnacceptedSubmissionHttpTest.code(
                context, MockMvcRequestBuilders.get(path, foreign).with(new AsVerifier())
            ),
            UnacceptedSubmissionHttpTest.code(
                context, MockMvcRequestBuilders.get(path, foreign).with(new AsCollector())
            ),
            UnacceptedSubmissionHttpTest.code(context, MockMvcRequestBuilders.get(path, own))
        );
    }

    private static UUID sent(final WebApplicationContext context) throws IOException {
        final UUID id = new SampleSubmissions(context)
            .add("SENT", SampleSubmissions.quick(UnacceptedSubmissionHttpTest.PHOTO));
        context.getBean(PhotoStore.class)
            .put(id, UnacceptedSubmissionHttpTest.PHOTO, JpegCleanerTest.photo());
        return id;
    }

    private static int code(
        final WebApplicationContext context, final MockHttpServletRequestBuilder request
    ) throws Exception {
        return context.getBean(MockMvc.class).perform(request)
            .andReturn().getResponse().getStatus();
    }
}
