/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sonda;

/**
 * Sonda.
 *
 * @since 1.0
 */
final class Sonda {
    /**
     * Sonda.
     * @param left Lewa
     * @param middle Srodek
     * @param right Prawa
     * @return Wynik
     */
    static boolean probe(final boolean left, final boolean middle, final boolean right) {
        return left || middle && right;
    }

    private Sonda() {
    }
}
