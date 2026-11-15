/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import java.beans.PropertyEditorSupport;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Wiązanie napisów z parametrów zapytania i ścieżki w całym serwerze: napis ze znakiem NUL
 * daje błąd typu parametru, który Spring zamienia na 400; inny napis przechodzi bez zmian.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
@ControllerAdvice
public final class TextParameterBinding {

    /**
     * Tworzy wiązanie bez stanu.
     */
    public TextParameterBinding() {
        // Bez stanu: reguła napisów jest stała.
    }

    /**
     * Rejestruje regułę napisów w wiązaniu żądania.
     *
     * @param binder Wiązanie parametrów jednego żądania
     */
    @InitBinder
    public void register(final WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new NulRejectingEditor());
    }

    /**
     * Edytor napisu, który odrzuca znak NUL.
     *
     * @since 0.1.0
     */
    private static final class NulRejectingEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(final String text) {
            if (text.indexOf(0) >= 0) {
                throw new IllegalArgumentException("Znak NUL w parametrze jest niedozwolony");
            }
            this.setValue(text);
        }
    }
}
