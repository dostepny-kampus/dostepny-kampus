/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov
 */
package io.github.dostepnykampus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punkt startowy serwera Dostępny Kampus.
 *
 * <p>Klasa nie ma stanu ani logiki. Jej pakiet jest pakietem bazowym:
 * Spring skanuje go razem z podpakietami w poszukiwaniu komponentów.</p>
 *
 * @since 0.1.0
 */
@SpringBootApplication(proxyBeanMethods = false)
public final class DostepnyKampusApplication {

    /**
     * Tworzy klasę konfiguracji.
     *
     * <p>Wywołuje ją tylko Spring przez refleksję, gdy rejestruje tę klasę
     * jako komponent konfiguracji.</p>
     */
    private DostepnyKampusApplication() {
    }

    /**
     * Uruchamia kontekst Springa i wbudowany serwer HTTP.
     *
     * <p>Metoda blokuje się do zatrzymania aplikacji. Otwiera port HTTP
     * z konfiguracji Spring Boot (domyślnie 8080).</p>
     *
     * @param args Argumenty wiersza poleceń przekazywane do Spring Boot
     */
    public static void main(final String... args) {
        SpringApplication.run(DostepnyKampusApplication.class, args);
    }
}
