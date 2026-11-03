/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import tools.jackson.databind.JsonNode;

/**
 * Reguły treści zgłoszenia zależne od rodzaju: pomiar precyzyjny (BR-7),
 * zgłoszenie szybkie (BR-8), „bariera usunięta” (BR-19) i zmiana oznaczenia
 * wejścia (BR-24), oraz zakresy pól ze schematu SubmissionContent.
 *
 * @since 0.1.0
 */
final class SubmissionRules {

    /**
     * Typ bariery z klasyfikacji barier, TB-1…TB-29.
     */
    private static final Pattern TYPE = Pattern.compile("TB-([1-9]|1[0-9]|2[0-9])");

    /**
     * Grupy barier ze schematu BarrierGroup.
     */
    private static final Set<String> GROUPS = Set.of(
        "ENTRANCE_AND_DOORS", "VERTICAL_CIRCULATION", "HORIZONTAL_CIRCULATION",
        "TOILETS", "ORIENTATION_AND_INFORMATION", "OTHER"
    );

    private SubmissionRules() {
    }

    /**
     * Sprawdza treść zgłoszenia o znanym rodzaju; błąd daje 400.
     *
     * @param body Treść zgłoszenia z poprawnym polem kind
     */
    static void check(final JsonNode body) {
        SubmissionRules.ranges(body);
        final String kind = body.path("kind").stringValue("");
        if ("PRECISE".equals(kind)) {
            SubmissionRules.precise(body);
        } else if ("QUICK".equals(kind)) {
            SubmissionDraft.require(
                body.has("barrierGroup"), "Zgłoszenie szybkie wymaga grupy barier (BR-8)"
            );
        } else {
            SubmissionDraft.require(
                body.path("entranceId").isString() && body.has("entranceMarking"),
                "Zmiana oznaczenia wejścia wymaga entranceId i entranceMarking (BR-24)"
            );
        }
    }

    private static void ranges(final JsonNode body) {
        final JsonNode severity = body.path("severity");
        SubmissionDraft.require(
            severity.isMissingNode()
                || severity.isInt() && severity.intValue() >= 1 && severity.intValue() <= 3,
            "Uciążliwość spoza zakresu 1–3"
        );
        SubmissionDraft.require(
            SubmissionRules.absentOr(
                body.path("barrierType"), SubmissionRules.TYPE.asMatchPredicate()
            ),
            "Typ bariery spoza TB-1…TB-29"
        );
        SubmissionDraft.require(
            SubmissionRules.absentOr(body.path("barrierGroup"), SubmissionRules.GROUPS::contains),
            "Nieznana grupa barier"
        );
        SubmissionDraft.require(
            SubmissionRules.absentOr(
                body.path("entranceMarking"), Set.of("ACCESSIBLE", "INACCESSIBLE")::contains
            ),
            "Nowe oznaczenie wejścia to ACCESSIBLE albo INACCESSIBLE"
        );
        SubmissionDraft.require(
            SubmissionRules.absentOr(body.path("description"), text -> text.length() <= 2000),
            "Opis dłuższy niż 2000 znaków"
        );
        SubmissionDraft.require(
            !body.path("barrierRemoved").booleanValue(false)
                || SubmissionDraft.uuid(body.path("replacesSubmissionId")),
            "„Bariera usunięta” wymaga zgłoszenia w polu replacesSubmissionId (BR-19)"
        );
    }

    private static void precise(final JsonNode body) {
        final JsonNode floor = body.path("floor");
        SubmissionDraft.require(
            body.has("barrierType"), "Pomiar precyzyjny wymaga typu bariery (BR-7)"
        );
        SubmissionDraft.require(
            body.has("severity") || body.path("barrierRemoved").booleanValue(false),
            "Pomiar precyzyjny wymaga uciążliwości (BR-7)"
        );
        SubmissionDraft.require(
            !body.has("buildingSymbol") || floor.path("floorId").isString()
                || floor.path("number").isInt(),
            "Pomiar precyzyjny w budynku wymaga piętra (BR-7)"
        );
        SubmissionDraft.require(
            !"TB-29".equals(body.path("barrierType").stringValue(""))
                || !body.path("description").stringValue("").isBlank(),
            "Typ TB-29 „inna bariera” wymaga opisu (US7.3)"
        );
    }

    private static boolean absentOr(final JsonNode node, final Predicate<String> valid) {
        return node.isMissingNode() || node.isString() && valid.test(node.stringValue());
    }
}
