/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier;

import java.util.List;

/**
 * Parametry zapytania GET /submissions z kontraktu API.
 *
 * @param buildingSymbol Tylko zgłoszenia z budynku o tym symbolu
 * @param floorId Tylko zgłoszenia z tego piętra budynku szczegółowego
 * @param status Tylko zgłoszenia w tych statusach
 * @param mine Tylko własne zgłoszenia; wymaga logowania
 * @since 0.1.0
 */
record SubmissionFilter(String buildingSymbol, String floorId, List<String> status, Boolean mine) {
}
