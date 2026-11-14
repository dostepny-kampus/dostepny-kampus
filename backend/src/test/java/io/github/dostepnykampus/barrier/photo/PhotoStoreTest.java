/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo;

import io.github.dostepnykampus.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Zdjęcie zgłoszenia zapisuje się w magazynie RustFS
 * pod identyfikatorami z telefonu, a ponowny zapis je zastępuje.
 *
 * <p>Wymagania: BR-7, BR-9</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
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
