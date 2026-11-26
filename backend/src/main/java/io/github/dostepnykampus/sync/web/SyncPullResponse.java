/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.web;

import io.github.dostepnykampus.shared.json.RawJson;
import java.util.List;

/**
 * Zmiany z serwera według schematu SyncPullResponse.
 *
 * <p>Wymagania: US15.1, US15.2, US8.1</p>
 *
 * @param cursor Kursor do parametru since w następnym pobraniu
 * @param submissions Własne zgłoszenia oraz przyjęte i archiwalne zmienione od kursora
 * @param entrances Wejścia ze zmienionym oznaczeniem, na razie zawsze puste
 * @param inspectionMarkers Znaczniki pięter zmienione od kursora
 * @since 0.1.0
 */
record SyncPullResponse(
    String cursor, List<RawJson> submissions, List<Object> entrances,
    List<SyncMarkerResponse> inspectionMarkers
) {
}
