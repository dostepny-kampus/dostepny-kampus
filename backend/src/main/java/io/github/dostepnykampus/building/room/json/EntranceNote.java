/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.json;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * Pole uwag wpisu warstwy wejść PWr, np. {@code Wejście Cesarskie. Od ul. Wybrzeże.}
 *
 * <p>Pierwsze zdanie jest nazwą, gdy zaczyna się od „Wejście”, ma najwyżej trzy słowa i nie
 * kończy się skrótem, np. „ul.”; reszta uwag to opis dojścia. Bez takiej nazwy nazwą jest
 * „Wejście”, a całe uwagi są opisem. Uwagi zaczynające się od „Wyjście ewakuacyjne” opisują
 * wyjście ewakuacyjne: nazwą jest „Wyjście ewakuacyjne”, a opisem całe uwagi.</p>
 *
 * <p>Wymagania: BR-27</p>
 *
 * @since 0.1.0
 */
final class EntranceNote {

    /**
     * Nazwa wejścia, gdy uwagi jej nie podają.
     */
    private static final String ENTRANCE = "Wejście";

    /**
     * Nazwa i początek uwag wyjścia ewakuacyjnego.
     */
    private static final String EXIT = "Wyjście ewakuacyjne";

    /**
     * Pierwsze zdanie i reszta uwag.
     */
    private static final Pattern SENTENCE = Pattern.compile(
        "^(.*?)[.!?](?:\\s+(.*))?$", Pattern.DOTALL
    );

    /**
     * Odstępy między słowami.
     */
    private static final Pattern SPACES = Pattern.compile("\\s+");

    /**
     * Skróty, po których kropka nie kończy zdania.
     */
    private static final Set<String> SHORTS = Set.of("ul", "al", "pl", "nr", "bud", "ok");

    /**
     * Najwięcej słów w nazwie wejścia.
     */
    private static final int WORDS = 3;

    /**
     * Uwagi bez spacji na brzegach.
     */
    private final String text;

    /**
     * Ctor.
     *
     * @param text Treść pola uwag, może być pusta
     */
    EntranceNote(final String text) {
        this.text = text.strip();
    }

    /**
     * Czy uwagi opisują wyjście ewakuacyjne.
     *
     * @return Prawda, gdy uwagi zaczynają się od „Wyjście ewakuacyjne”
     */
    boolean exit() {
        return this.text.toLowerCase(Locale.ROOT)
            .startsWith(EntranceNote.EXIT.toLowerCase(Locale.ROOT));
    }

    /**
     * Nazwa wpisu.
     *
     * @return Pierwsze zdanie, gdy jest nazwą, „Wyjście ewakuacyjne” albo „Wejście”
     */
    String name() {
        final String result;
        if (this.exit()) {
            result = EntranceNote.EXIT;
        } else if (this.named()) {
            result = this.first();
        } else {
            result = EntranceNote.ENTRANCE;
        }
        return result;
    }

    /**
     * Opis dojścia.
     *
     * @return Uwagi bez nazwy albo null, gdy nic poza nazwą nie zostaje
     */
    @Nullable String approach() {
        final String rest;
        if (!this.exit() && this.named()) {
            rest = this.rest();
        } else {
            rest = this.text;
        }
        final String result;
        if (rest.isEmpty()) {
            result = null;
        } else {
            result = rest;
        }
        return result;
    }

    private boolean named() {
        final String first = this.first();
        final String[] words = EntranceNote.SPACES.split(first);
        return first.startsWith(EntranceNote.ENTRANCE) && words.length <= EntranceNote.WORDS
            && !EntranceNote.SHORTS.contains(words[words.length - 1].toLowerCase(Locale.ROOT));
    }

    private String first() {
        final Matcher sentence = EntranceNote.SENTENCE.matcher(this.text);
        final String result;
        if (sentence.matches()) {
            result = sentence.group(1).strip();
        } else {
            result = this.text;
        }
        return result;
    }

    private String rest() {
        final Matcher sentence = EntranceNote.SENTENCE.matcher(this.text);
        final String result;
        if (sentence.matches() && sentence.group(2) != null) {
            result = sentence.group(2).strip();
        } else {
            result = "";
        }
        return result;
    }
}
