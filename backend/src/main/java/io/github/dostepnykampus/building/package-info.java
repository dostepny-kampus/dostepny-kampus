/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł building: budynki kampusu, ich wejścia, piętra i pomieszczenia.
 *
 * <p>Obsługuje wyszukanie budynku, kartę budynku z wejściami,
 * dobór wejścia dostępnego, piętra i plany pięter oraz
 * oznaczenie piętra obejrzanego.</p>
 *
 * <p>Wymagania: US1, US2, US17, US3, US16, US15</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {})
@NullMarked
package io.github.dostepnykampus.building;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
