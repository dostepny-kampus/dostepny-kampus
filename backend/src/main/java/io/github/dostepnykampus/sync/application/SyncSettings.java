/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ustawienia pobrania zmian {@code dk.sync}. Margines kursora musi być dłuższy niż limit czasu
 * transakcji zapisu ({@code spring.transaction.default-timeout}); margines zerowy albo ujemny
 * kończy się wyjątkiem IllegalArgumentException.
 *
 * <p>Wymagania: US15.1, US15.2, US8.1</p>
 *
 * @param cursorMargin O ile kursor odpowiedzi jest cofnięty względem chwili pobrania
 * @since 0.1.0
 */
@ConfigurationProperties("dk.sync")
public record SyncSettings(Duration cursorMargin) {

    /**
     * Sprawdza margines kursora.
     */
    public SyncSettings {
        if (cursorMargin.isNegative() || cursorMargin.isZero()) {
            throw new IllegalArgumentException(
                "dk.sync.cursor-margin: margines nie jest dodatni: %s".formatted(cursorMargin)
            );
        }
    }
}
