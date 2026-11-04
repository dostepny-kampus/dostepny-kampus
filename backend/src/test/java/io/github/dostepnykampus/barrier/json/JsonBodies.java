/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import io.github.dostepnykampus.StrictMapper;
import java.util.UUID;
import java.util.function.Consumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Wspólne ciała JSON i odczyty testów treści zgłoszenia; apostrofy w treści
 * JSON to cudzysłowy.
 *
 * @since 0.1.0
 */
final class JsonBodies {

    /**
     * Zgłoszenie zastępowane w pełnej treści.
     */
    static final UUID REPLACED = UUID.fromString("7a1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Zdjęcie w treści.
     */
    static final UUID PHOTO = UUID.fromString("5d1c2b3a-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    /**
     * Pełna treść z każdym polem SubmissionContent.
     */
    static final String FULL = String.join(
        "",
        "{'kind':'PRECISE','location':{'type':'Point','coordinates':[17.06,51.1]},",
        "'accuracyMeters':4.5,'manualCorrection':true,'buildingSymbol':'C-13',",
        "'floor':{'floorId':'p1','number':1,'mezzanine':true},",
        "'barrierGroup':'VERTICAL_CIRCULATION','barrierType':'TB-29','severity':3,",
        "'description':'opis','replacesSubmissionId':'%s','barrierRemoved':true,",
        "'entranceId':'node/7','entranceMarking':'ACCESSIBLE','photoIds':['%s']}"
    ).formatted(JsonBodies.REPLACED, JsonBodies.PHOTO);

    /**
     * Najmniejsza poprawna treść zgłoszenia szybkiego.
     */
    static final String QUICK = String.join(
        "",
        "{'kind':'QUICK','location':{'type':'Point','coordinates':[17.06,51.1]},",
        "'barrierGroup':'OTHER','photoIds':['%s']}"
    ).formatted(JsonBodies.PHOTO);

    /**
     * Mapper ze ścisłym wiązaniem jak na serwerze.
     */
    private static final JsonMapper MAPPER = new StrictMapper().get();

    private JsonBodies() {
    }

    static ObjectNode with(final Consumer<ObjectNode> change) {
        final ObjectNode node = JsonBodies.MAPPER.readValue(
            JsonBodies.QUICK.replace('\'', '"'), ObjectNode.class
        );
        change.accept(node);
        return node;
    }

    static JsonNode json(final String text) {
        return JsonBodies.MAPPER.readTree(text.replace('\'', '"'));
    }

    static SubmissionContentJson bind(final JsonNode node) {
        return JsonBodies.MAPPER.readValue(node.toString(), SubmissionContentJson.class);
    }
}
