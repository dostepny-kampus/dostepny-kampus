/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.web;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ustawienia limitu żądań bez logowania {@code dk.rate-limit}. Limit mniejszy niż 1 kończy się
 * wyjątkiem IllegalArgumentException; brak listy adresów bez limitu znaczy listę pustą.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @param publicPerMinute Największa liczba żądań bez logowania z jednego adresu na minutę
 * @param unlimitedAddresses Adresy klientów bez limitu, np. adres monitoringu
 * @since 0.1.0
 */
@ConfigurationProperties("dk.rate-limit")
public record RateLimitSettings(int publicPerMinute, List<String> unlimitedAddresses) {

    /**
     * Sprawdza limit i kopiuje listę adresów.
     */
    public RateLimitSettings {
        if (publicPerMinute < 1) {
            throw new IllegalArgumentException(
                "dk.rate-limit.public-per-minute: limit mniejszy niż 1: %d".formatted(publicPerMinute)
            );
        }
        unlimitedAddresses = List.copyOf(Objects.requireNonNullElse(unlimitedAddresses, List.of()));
    }
}
