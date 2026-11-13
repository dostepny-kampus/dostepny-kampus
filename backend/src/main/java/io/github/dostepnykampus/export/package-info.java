/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł export: postęp inwentaryzacji i eksport dla Koordynatora.
 *
 * <p>Obsługuje postęp inwentaryzacji według budynków i eksport zgłoszeń
 * przyjętych do otwarcia w QGIS.</p>
 *
 * <p>Wymagania: US11, US13</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(allowedDependencies = {})
@NullMarked
package io.github.dostepnykampus.export;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
