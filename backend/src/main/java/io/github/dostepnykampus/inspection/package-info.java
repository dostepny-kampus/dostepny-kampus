/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł inspection obejmuje znaczniki obejrzanych pięter i postęp inwentaryzacji.
 *
 * <p>Zapis znaczników obejrzanych pięter i odczyt postępu budynków.</p>
 *
 * <p>API: HTTP {@code /sync/push}, {@code DELETE /inspection-markers/{id}}
 * i {@code GET /progress}.</p>
 *
 * <p>Zależności: {@code building :: api}, {@code shared}.</p>
 *
 * <p>Nie robi: nie przyjmuje zgłoszeń barier.</p>
 *
 * <p>Kiedy dodawać tutaj: znaczniki obejrzanych pięter i postęp budynków.</p>
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, US15, BR-20, BR-21, BR-24</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {"building :: api", "shared"})
@NullMarked
package io.github.dostepnykampus.inspection;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
