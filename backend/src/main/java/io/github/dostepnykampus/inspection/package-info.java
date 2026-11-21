/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł inspection: znaczniki pięter obejrzanych przez Zbierających.
 *
 * <p>Zadanie: zapis znaczników i odczyt postępu inwentaryzacji.</p>
 *
 * <p>API: {@code /sync/push}, {@code /inspection-markers/{id}}, {@code /progress}.</p>
 *
 * <p>Zależności: {@code building :: api}, {@code shared}.</p>
 *
 * <p>Nie robi: nie przyjmuje zgłoszeń barier.</p>
 *
 * <p>Kiedy dodawać tutaj: kod znaczników piętra; zgłoszenia barier zostają w module barrier.</p>
 *
 * <p>Wymagania: US15, BR-20</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {"building :: api", "shared"})
@NullMarked
package io.github.dostepnykampus.inspection;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
