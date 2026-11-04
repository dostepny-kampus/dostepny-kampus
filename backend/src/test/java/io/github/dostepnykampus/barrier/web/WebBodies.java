/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.StrictMapper;
import io.github.dostepnykampus.barrier.domain.BarrierDetails;
import io.github.dostepnykampus.barrier.domain.BarrierGroup;
import io.github.dostepnykampus.barrier.domain.SubmissionContent;
import io.github.dostepnykampus.barrier.domain.SubmissionKind;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.util.List;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wspólne identyfikatory, ciała JSON i treść domeny testów ciał HTTP zgłoszeń;
 * apostrofy w treści JSON to cudzysłowy.
 *
 * @since 0.1.0
 */
final class WebBodies {

    /**
     * Identyfikator zgłoszenia w ciele.
     */
    static final UUID ID = UUID.fromString("0b1c2d3e-4f50-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Identyfikator zgłoszenia w adresie, inny niż w ciele.
     */
    static final UUID PATH = UUID.fromString("9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d");

    /**
     * Zdjęcie w treści.
     */
    static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Mapper ze ścisłym wiązaniem jak na serwerze.
     */
    static final JsonMapper MAPPER = new StrictMapper().get();

    private WebBodies() {
    }

    static String newBody(final String tail) {
        return WebBodies.body(String.format("'id':'%s',%s", WebBodies.ID, tail));
    }

    static String body(final String head) {
        return String.format(
            "{%s,'kind':'QUICK','location':{'type':'Point','coordinates':[17.06,51.1]},%s}",
            head, String.format("'barrierGroup':'OTHER','photoIds':['%s']", WebBodies.PHOTO)
        ).replace('\'', '"');
    }

    static SubmissionContent content() {
        return new SubmissionContent(
            SubmissionKind.QUICK,
            new SubmissionPlace(new SubmissionPoint(17.06, 51.1), null, false, null, null),
            new BarrierDetails(BarrierGroup.OTHER, null, null, null, false),
            null, null, null, List.of(WebBodies.PHOTO)
        );
    }
}
