/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.identity.account;

/**
 * Rola pracownicza nadawana zaproszeniem; nazwa jest nazwą roli realmu.
 *
 * <p>Wymagania: BR-2, US12.1</p>
 *
 * @since 0.1.0
 */
public enum StaffRole {
    /** Zbierający. */
    COLLECTOR,

    /** Weryfikator. */
    VERIFIER,

    /** Koordynator. */
    COORDINATOR
}
