/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Aktywny znacznik piętra albo budynku w postępie inwentaryzacji z pseudonimem autora.
 *
 * <p>Wymagania: US11.3, US15.2, BR-5, BR-20</p>
 *
 * @param id Identyfikator znacznika
 * @param place Budynek albo piętro znacznika
 * @param created Chwila odbioru znacznika przez serwer
 * @param mine Czy znacznik postawiła osoba czytająca postęp
 * @param pseudonym Pseudonim autora bez danych osobowych
 * @since 0.1.0
 */
public record ProgressMarker(
    UUID id,
    MarkerPlace place,
    Instant created,
    boolean mine,
    String pseudonym
) {
}
