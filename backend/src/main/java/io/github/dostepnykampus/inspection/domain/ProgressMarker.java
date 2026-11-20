/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Aktywny znacznik piętra albo budynku w postępie inwentaryzacji z pseudonimem autora.
 *
 * <p>Wymagania: US11.3, US15.2, BR-5, BR-20</p>
 *
 * @param id Identyfikator znacznika
 * @param building Symbol budynku
 * @param floor Identyfikator piętra albo null dla budynku podstawowego
 * @param created Chwila odbioru znacznika przez serwer
 * @param mine Czy znacznik postawiła osoba czytająca postęp
 * @param pseudonym Pseudonim autora bez danych osobowych
 * @since 0.1.0
 */
public record ProgressMarker(
    UUID id,
    String building,
    @Nullable String floor,
    Instant created,
    boolean mine,
    String pseudonym
) {
}
