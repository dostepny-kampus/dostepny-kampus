/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import java.util.UUID;

/**
 * Operacja paczki po odczycie: poprawna albo z błędem danych, który odrzuca tylko ją.
 *
 * <p>Wymagania: US9.2, BR-9</p>
 *
 * @since 0.1.0
 */
sealed interface ReadOperation permits ReadOperation.Valid, ReadOperation.Broken {

    /**
     * Operacja z poprawnymi danymi.
     *
     * @param operation Operacja do wykonania
     * @since 0.1.0
     */
    record Valid(SyncOperationJson operation) implements ReadOperation {
    }

    /**
     * Operacja z błędem danych.
     *
     * @param id Identyfikator operacji z telefonu
     * @param detail Opis błędu dla telefonu
     * @since 0.1.0
     */
    record Broken(UUID id, String detail) implements ReadOperation {
    }
}
