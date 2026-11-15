/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import java.beans.PropertyEditorSupport;

/**
 * Edytor napisu z parametru żądania: napis ze znakiem NUL kończy się wyjątkiem
 * IllegalArgumentException, inny napis przechodzi bez zmian.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class NulRejectingEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(final String text) {
        if (text.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Znak NUL w parametrze jest niedozwolony");
        }
        this.setValue(text);
    }
}
