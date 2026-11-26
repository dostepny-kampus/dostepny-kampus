/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.shared.json;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gotowy dokument JSON, który zapis odpowiedzi wstawia bez zmian. Moduł właściciel danych
 * zapisuje w nim swój obiekt w kształcie kontraktu, a inny moduł tylko go przekazuje.
 *
 * @param value Dokument JSON
 * @since 0.1.0
 */
public record RawJson(@JsonValue @JsonRawValue String value) {
}
