/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import java.beans.PropertyEditor;
import java.util.function.Supplier;
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
     * Źródło edytorów napisu; edytor ma stan, więc każde wiązanie dostaje nowy.
     */
    private final Supplier<PropertyEditor> editors;

    /**
     * Tworzy wiązanie z edytorem odrzucającym znak NUL.
     */
    public TextParameterBinding() {
        this.editors = NulRejectingEditor::new;
    }

    /**
     * Rejestruje regułę napisów w wiązaniu parametrów jednego żądania.
     *
     * @param binder Wiązanie parametrów żądania
     */
    @InitBinder
    public void register(final WebDataBinder binder) {
        binder.registerCustomEditor(String.class, this.editors.get());
    }
}
