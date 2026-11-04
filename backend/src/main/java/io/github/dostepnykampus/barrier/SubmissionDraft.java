/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Nowe zgłoszenie z telefonu po sprawdzeniu pól wymaganych przez schemat
 * NewSubmission: identyfikatora, rodzaju, miejsca i zdjęć (BR-7, BR-8).
 *
 * @param id Identyfikator nadany w telefonie
 * @param kind Rodzaj zgłoszenia: PRECISE, QUICK albo ENTRANCE_MARKING
 * @param lat Szerokość geograficzna miejsca zgłoszenia
 * @param lon Długość geograficzna miejsca zgłoszenia
 * @param content Treść zgłoszenia bez identyfikatora i pól spoza schematu
 * @since 0.1.0
 */
record SubmissionDraft(UUID id, String kind, double lat, double lon, ObjectNode content) {

    /**
     * Rodzaje zgłoszenia ze schematu SubmissionContent.
     */
    private static final Set<String> KINDS = Set.of("PRECISE", "QUICK", "ENTRANCE_MARKING");

    /**
     * Pola schematu SubmissionContent; pozostałe pola serwer pomija.
     */
    private static final Set<String> FIELDS = Set.of(
        "kind", "location", "accuracyMeters", "manualCorrection", "buildingSymbol",
        "floor", "barrierGroup", "barrierType", "severity", "description",
        "replacesSubmissionId", "barrierRemoved", "entranceId", "entranceMarking",
        "photoIds"
    );

    /**
     * Zapis UUID w formacie z kontraktu.
     */
    private static final Pattern UUID_TEXT = Pattern.compile(
        "[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"
    );

    /**
     * Sprawdza treść żądania i wyciąga z niej pola potrzebne serwerowi.
     *
     * @param body Treść żądania POST /submissions
     * @return Zgłoszenie do zapisu
     */
    static SubmissionDraft of(final JsonNode body) {
        SubmissionDraft.require(
            SubmissionDraft.uuid(body.path("id")), "Brak UUID zgłoszenia w polu id"
        );
        return SubmissionDraft.of(UUID.fromString(body.path("id").stringValue()), body);
    }

    /**
     * Sprawdza treść zgłoszenia o znanym identyfikatorze, np. edycji.
     *
     * <p>Strukturę miejsca (punkt GeoJSON z dwiema liczbami) sprawdza ta
     * metoda, zakres WGS84 — {@link SubmissionPoint}.</p>
     *
     * @param id Identyfikator zgłoszenia
     * @param body Treść zgłoszenia według schematu SubmissionContent
     * @return Zgłoszenie do zapisu
     * @throws io.github.dostepnykampus.barrier.domain.InvalidSubmission Gdy
     *  współrzędne miejsca są poza zakresem WGS84
     */
    static SubmissionDraft of(final UUID id, final JsonNode body) {
        SubmissionDraft.require(body.isObject(), "Zgłoszenie nie jest obiektem JSON");
        SubmissionDraft.require(
            SubmissionDraft.KINDS.contains(body.path("kind").stringValue("")),
            "Nieznany rodzaj zgłoszenia w polu kind"
        );
        final JsonNode place = body.path("location").path("coordinates");
        SubmissionDraft.require(
            "Point".equals(body.path("location").path("type").stringValue(""))
                && place.size() == 2
                && place.path(0).isNumber()
                && place.path(1).isNumber(),
            "Miejsce zgłoszenia nie jest punktem GeoJSON z dwiema współrzędnymi"
        );
        final SubmissionPoint point = new SubmissionPoint(
            place.path(0).doubleValue(), place.path(1).doubleValue()
        );
        final JsonNode photos = body.path("photoIds");
        SubmissionDraft.require(
            photos.isArray() && !photos.isEmpty()
                && StreamSupport.stream(photos.spliterator(), false)
                    .allMatch(SubmissionDraft::uuid),
            "Zgłoszenie wymaga co najmniej jednego UUID zdjęcia w polu photoIds"
        );
        SubmissionDraft.require(
            body.path("buildingSymbol").isMissingNode() || body.path("buildingSymbol").isString(),
            "Symbol budynku nie jest tekstem"
        );
        SubmissionRules.check(body);
        return new SubmissionDraft(
            id,
            body.path("kind").stringValue(),
            point.latitude(),
            point.longitude(),
            ((ObjectNode) body).deepCopy().retain(SubmissionDraft.FIELDS)
        );
    }

    /**
     * Sprawdza, czy węzeł to UUID zapisany tekstem.
     *
     * @param node Węzeł JSON
     * @return Czy węzeł to UUID
     */
    static boolean uuid(final JsonNode node) {
        return SubmissionDraft.UUID_TEXT.matcher(node.stringValue("")).matches();
    }

    /**
     * Odrzuca zgłoszenie z błędem 400, gdy reguła nie jest spełniona.
     *
     * @param valid Czy reguła jest spełniona
     * @param reason Opis złamanej reguły do odpowiedzi
     */
    static void require(final boolean valid, final String reason) {
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
        }
    }
}
