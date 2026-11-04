/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.UUID;

/**
 * Komenda edycji zgłoszenia, schemat SubmissionUpdate w OpenAPI (BR-10).
 *
 * @param id Identyfikator edytowanego zgłoszenia
 * @param expectedVersion Wersja, na której oparto edycję, co najmniej 1
 * @param content Nowa treść zgłoszenia
 * @since 0.1.0
 */
public record SubmissionEdit(UUID id, int expectedVersion, SubmissionContent content) {
}
