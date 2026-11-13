/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł shared: typy wspólne dla wszystkich modułów serwera.
 *
 * <p>Zadanie: typy błędów, ścisłe wiązanie JSON i układy współrzędnych
 * używane przez każdy moduł.</p>
 *
 * <p>API: wszystkie typy podpakietów {@code error}, {@code json} i
 * {@code geo}; moduł jest otwarty.</p>
 *
 * <p>Zależności: brak.</p>
 *
 * <p>Nie robi: nie ma logiki dziedziny, kontrolerów ani zapisu do
 * bazy.</p>
 *
 * <p>Kiedy dodawać tutaj: typ używany przez co najmniej dwa moduły, który
 * pasuje do {@code error}, {@code json} albo {@code geo}. Inny temat to
 * nowy moduł, nie nowy podpakiet.</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
@NullMarked
package io.github.dostepnykampus.shared;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
