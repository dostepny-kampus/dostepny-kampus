/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Typ bariery z klasyfikacji barier TB-1…TB-29, schemat BarrierType
 * w OpenAPI (US7.3).
 *
 * <p>Każda droga utworzenia przyjmuje tylko numer od 1 do 29; poza nim
 * rzuca {@link InvalidSubmission} z opisem „Typ bariery spoza
 * TB-1…TB-29”.</p>
 *
 * @param number Numer typu w klasyfikacji, od 1 do 29
 * @since 0.1.0
 */
public record BarrierType(int number) {

    /**
     * Typ z kodu zgodnego z wzorcem schematu BarrierType: wielkie litery,
     * bez zer wiodących i bez spacji; inny kod daje
     * {@link InvalidSubmission}.
     *
     * @param code Kod typu, np. TB-3
     */
    public BarrierType(final String code) {
        this(0);
    }

    /**
     * Kod typu w postaci ze schematu BarrierType.
     *
     * @return Kod TB-n, gdzie n to numer typu
     */
    public String code() {
        return "";
    }

    /**
     * Czy to typ „inna bariera”, który wymaga opisu (US7.3).
     *
     * @return Prawda tylko dla TB-29
     */
    public boolean other() {
        return false;
    }
}
