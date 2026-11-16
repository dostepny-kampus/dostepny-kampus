/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * Ciało POST /sync/push: paczka zmian z telefonu w kolejności zapisu.
 *
 * @param operations Operacje jeszcze niezwiązane, czytane osobno, albo null, gdy nie podano
 * @since 0.1.0
 */
record SyncPushRequest(@Nullable List<JsonNode> operations) {

    /**
     * Sprawdza liczbę operacji w paczce: od 1 do 100.
     *
     * @return Operacje w kolejności zapisu
     */
    List<JsonNode> checked() {
        if (this.operations == null || this.operations.isEmpty() || this.operations.size() > 100) {
            throw new InvalidInput("Paczka ma od 1 do 100 operacji w polu operations");
        }
        return this.operations;
    }
}
