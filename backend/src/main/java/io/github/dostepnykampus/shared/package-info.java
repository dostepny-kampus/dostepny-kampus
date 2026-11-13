/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */

/**
 * Moduł shared: typy wspólne dla wszystkich modułów serwera.
 *
 * <p>Moduł otwarty: inne moduły używają typów z podpakietów {@code error},
 * {@code json} i {@code geo} bezpośrednio. Inne podpakiety nie powstają.
 * Moduł nie ma logiki dziedziny ani kontrolerów.</p>
 *
 * @since 0.1.0
 */
@ApplicationModule(type = ApplicationModule.Type.OPEN, allowedDependencies = {})
@NullMarked
package io.github.dostepnykampus.shared;

import org.jspecify.annotations.NullMarked;
import org.springframework.modulith.ApplicationModule;
