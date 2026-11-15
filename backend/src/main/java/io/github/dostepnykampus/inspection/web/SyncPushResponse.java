/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import java.util.List;

/**
 * Odpowiedź POST /sync/push z wynikiem każdej operacji w kolejności żądania.
 *
 * @param results Wyniki operacji
 * @since 0.1.0
 */
record SyncPushResponse(List<SyncResultResponse> results) {
}
