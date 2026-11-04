/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import io.github.dostepnykampus.barrier.domain.NewEntranceMarking;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Treść zgłoszenia w JSON, schemat SubmissionContent w OpenAPI. Pola miejsca
 * i bariery są rozpakowane z zagnieżdżonych rekordów, bo qulice dopuszcza
 * do ośmiu komponentów rekordu; format JSON jest płaski.
 *
 * @param kind Rodzaj zgłoszenia
 * @param place Pola miejsca zgłoszenia
 * @param barrier Pola bariery
 * @param replaces Zastępowane zgłoszenie, w JSON replacesSubmissionId
 * @param entranceId Wejście w zmianie oznaczenia albo null
 * @param entranceMarking Nowe oznaczenie wejścia albo null
 * @param photoIds Identyfikatory zdjęć
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubmissionContentJson(
    @Nullable SubmissionKind kind,
    @JsonUnwrapped PlaceJson place,
    @JsonUnwrapped BarrierJson barrier,
    @JsonProperty("replacesSubmissionId") @Nullable UUID replaces,
    @Nullable String entranceId,
    @Nullable NewEntranceMarking entranceMarking,
    @Nullable List<UUID> photoIds
) {

    /**
     * Zapisuje treść zgłoszenia w JSON.
     *
     * @param content Treść zgłoszenia
     */
    public SubmissionContentJson(final SubmissionContent content) {
        this(
            content.kind(), new PlaceJson(content.place()), new BarrierJson(content.barrier()),
            content.replaces(), content.entranceId(), content.entranceMarking(),
            content.photoIds()
        );
    }

    /**
     * Przenosi treść z JSON do domeny.
     *
     * @return Treść zgłoszenia
     * @throws InvalidSubmission Gdy brak pola wymaganego albo wartość narusza
     *  inwariant domeny
     */
    public SubmissionContent toContent() throws InvalidSubmission {
        if (this.kind == null) {
            throw new InvalidSubmission("Zgłoszenie wymaga pola kind");
        }
        if (this.photoIds == null) {
            throw new InvalidSubmission("Zgłoszenie wymaga pola photoIds");
        }
        return new SubmissionContent(
            this.kind, this.place.toPlace(), this.barrier.toDetails(), this.entranceId,
            this.entranceMarking, this.replaces, this.photoIds
        );
    }
}
