/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.PostgisTestConfiguration;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

/**
 * Testy BR-7 i BR-9: zdjęcie zgłoszenia zapisuje się w magazynie RustFS
 * pod identyfikatorami z telefonu, a ponowny zapis je zastępuje.
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import({PostgisTestConfiguration.class, RustfsTestConfiguration.class})
final class PhotoStoreTest {

    @Test
    void brSevenReadsSavedPhoto(final ApplicationContext context) {
        final PhotoStore store = context.getBean(PhotoStore.class);
        final UUID submission = UUID.randomUUID();
        store.put(submission, submission, PhotoStoreTest.bytes("zdjęcie"));
        MatcherAssert.assertThat(
            "Odczytane zdjęcie różni się od zapisanego",
            store.get(submission, submission).map(PhotoStoreTest::text),
            Matchers.equalTo(Optional.of("zdjęcie"))
        );
    }

    @Test
    void brNineReplacesPhotoSentAgain(final ApplicationContext context) {
        final PhotoStore store = context.getBean(PhotoStore.class);
        final UUID submission = UUID.randomUUID();
        store.put(submission, submission, PhotoStoreTest.bytes("pierwsze"));
        store.put(submission, submission, PhotoStoreTest.bytes("drugie"));
        MatcherAssert.assertThat(
            "Ponowny zapis zdjęcia nie zastąpił poprzedniego",
            store.get(submission, submission).map(PhotoStoreTest::text),
            Matchers.equalTo(Optional.of("drugie"))
        );
    }

    @Test
    void brSevenFindsNoMissingPhoto(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Magazyn zwrócił zdjęcie, którego nikt nie zapisał",
            context.getBean(PhotoStore.class).get(UUID.randomUUID(), UUID.randomUUID()),
            Matchers.equalTo(Optional.empty())
        );
    }

    private static byte[] bytes(final String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String text(final byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
