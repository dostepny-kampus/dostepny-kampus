/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Typ bariery z klasyfikacji barier TB-1…TB-29, schemat BarrierType
 * w OpenAPI.
 *
 * <p>Każda droga utworzenia przyjmuje tylko numer od 1 do 29; poza nim
 * rzuca {@link InvalidSubmission} z opisem „Typ bariery spoza
 * TB-1…TB-29”.</p>
 *
 * <p>Wymagania: US7.3</p>
 *
 * @param number Numer typu w klasyfikacji, od 1 do 29
 * @since 0.1.0
 */
public record BarrierType(int number) {

    /**
     * Wzorzec schematu BarrierType w OpenAPI, tylko cyfry ASCII.
     */
    private static final Pattern CODE = Pattern.compile("TB-([1-9]|1[0-9]|2[0-9])");

    /**
     * Opis błędu typu spoza klasyfikacji.
     */
    private static final String OUTSIDE = "Typ bariery spoza TB-1…TB-29";

    /**
     * Typ o numerze z klasyfikacji.
     *
     * @throws InvalidSubmission Gdy numer jest poza zakresem 1…29
     */
    public BarrierType {
        if (number < 1 || number > 29) {
            throw new InvalidSubmission(BarrierType.OUTSIDE);
        }
    }

    /**
     * Typ z kodu zgodnego z wzorcem schematu BarrierType: wielkie litery,
     * bez zer wiodących i bez spacji; inny kod daje
     * {@link InvalidSubmission}.
     *
     * @param code Kod typu, np. TB-3
     * @throws InvalidSubmission Gdy kod nie pasuje do wzorca
     */
    public BarrierType(final String code) throws InvalidSubmission {
        this(BarrierType.numberOf(code));
    }

    /**
     * Kod typu w postaci ze schematu BarrierType.
     *
     * @return Kod TB-n, gdzie n to numer typu
     */
    public String code() {
        return String.format("TB-%d", this.number);
    }

    /**
     * Czy to typ „inna bariera”, który wymaga opisu.
     *
     * <p>Wymagania: US7.3</p>
     *
     * @return Prawda tylko dla TB-29
     */
    public boolean other() {
        return this.number == 29;
    }

    private static int numberOf(final String code) {
        final Matcher matcher = BarrierType.CODE.matcher(code);
        if (!matcher.matches()) {
            throw new InvalidSubmission(BarrierType.OUTSIDE);
        }
        return Integer.parseInt(matcher.group(1));
    }
}
