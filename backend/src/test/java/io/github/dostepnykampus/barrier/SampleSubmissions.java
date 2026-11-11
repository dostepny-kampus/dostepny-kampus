/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.NewSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import io.github.dostepnykampus.barrier.persistence.JdbcSubmissions;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Zgłoszenia próbne w bazie (#842): wiersz i treść JSON zapisuje produkcyjny
 * {@link JdbcSubmissions}, a test ustawia tylko status.
 *
 * <p>Treść powstaje z próbki dziedziny, więc nowe pole treści albo nowa kolumna
 * zgłoszenia nie wymaga zmian w testach, które z niej korzystają.</p>
 *
 * @since 0.1.0
 */
final class SampleSubmissions {

    /**
     * Kontekst aplikacji z bazą.
     */
    private final ApplicationContext context;

    /**
     * Zgłoszenia próbne w bazie aplikacji.
     *
     * @param context Kontekst aplikacji z bazą
     */
    SampleSubmissions(final ApplicationContext context) {
        this.context = context;
    }

    /**
     * Szybkie zgłoszenie bariery „inne” w punkcie (17; 51), poza budynkiem.
     *
     * @param photo Jedyne zdjęcie zgłoszenia
     * @return Treść zgłoszenia
     */
    static SubmissionContent quick(final UUID photo) {
        return SampleSubmissions.quick(
            new SubmissionPlace(new SubmissionPoint(17.0, 51.0), null, false, null, null),
            null, photo
        );
    }

    /**
     * Szybkie zgłoszenie bariery „inne” w miejscu.
     *
     * @param place Miejsce zgłoszenia
     * @param description Opis albo {@code null}
     * @param photo Jedyne zdjęcie zgłoszenia
     * @return Treść zgłoszenia
     */
    static SubmissionContent quick(
        final SubmissionPlace place, final @Nullable String description, final UUID photo
    ) {
        return new SubmissionContent(
            SubmissionKind.QUICK, place,
            new BarrierDetails(BarrierGroup.OTHER, null, null, description, false),
            null, null, null, List.of(photo)
        );
    }

    /**
     * Zapisuje zgłoszenie z treścią i ustawia mu status.
     *
     * @param status Status zgłoszenia, np. "SENT"
     * @param content Treść zgłoszenia
     * @return Identyfikator zgłoszenia
     */
    UUID add(final String status, final SubmissionContent content) {
        return this.add(UUID.randomUUID(), status, content);
    }

    /**
     * Zapisuje zgłoszenie o danym identyfikatorze z treścią i ustawia mu status.
     *
     * @param id Identyfikator zgłoszenia
     * @param status Status zgłoszenia, np. "SENT"
     * @param content Treść zgłoszenia
     * @return Identyfikator zgłoszenia
     */
    UUID add(final UUID id, final String status, final SubmissionContent content) {
        this.context.getBean(JdbcSubmissions.class).create(new NewSubmission(id, content));
        this.context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET status = :status WHERE id = :id")
            .param("status", status).param("id", id).update();
        return id;
    }

    /**
     * Ustawia wersję zapisanego zgłoszenia, jak po kolejnych edycjach.
     *
     * @param id Identyfikator zgłoszenia
     * @param version Wersja, od 1
     */
    void version(final UUID id, final int version) {
        this.context.getBean(JdbcClient.class)
            .sql("UPDATE submissions SET version = :version WHERE id = :id")
            .param("version", version).param("id", id).update();
    }
}
