/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.photo.application;

import java.util.UUID;

/**
 * Zapis zdjęcia zgłoszenia przez konto: które zdjęcie którego zgłoszenia i kto je wysyła.
 *
 * <p>Wymagania: BR-3, NFR-25</p>
 *
 * @param submission Identyfikator zgłoszenia
 * @param photo Identyfikator zdjęcia z listy photoIds
 * @param account Identyfikator konta z tokenu
 * @since 0.1.0
 */
public record PhotoWrite(UUID submission, UUID photo, String account) {
}
