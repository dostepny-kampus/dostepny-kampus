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
 * kończy się skrótem, np. „ul.”; reszta uwag to opis dojścia. Bez takiej nazwy wpis nie ma
 * nazwy, a całe uwagi są opisem. Uwagi zaczynające się od „Wyjście ewakuacyjne” albo
 * „Wyjście pełni wyłącznie funkcję ewakuacyjną” opisują wyjście ewakuacyjne: nazwą jest
 * „Wyjście ewakuacyjne”, a opisem całe uwagi.</p>
 *
 * <p>Wymagania: BR-27</p>
 *
 * @since 0.1.0
 */
final class EntranceNote {

    /**
     * Nazwa wyjścia ewakuacyjnego.
     */
    private static final String EXIT = "Wyjście ewakuacyjne";

    /**
     * Początki uwag wyjścia ewakuacyjnego, małymi literami.
     */
    private static final Set<String> EXITS = Set.of(
        "wyjście ewakuacyjne", "wyjście pełni wyłącznie funkcję ewakuacyjną"
    );

    /**
     * Pierwsze zdanie i reszta uwag.
     */
    private static final Pattern SENTENCE = Pattern.compile(
        "^(.*?)[.!?](?:\\s+(.*))?$", Pattern.DOTALL
    );

    /**
     * Nazwa wejścia: „Wejście” i najwyżej dwa dalsze słowa, ostatnie słowo w grupie.
     */
    private static final Pattern NAME = Pattern.compile(
        "Wejście(?:\\s+\\S+)??(?:\\s+(\\S+))?"
    );

    /**
     * Skróty, po których kropka nie kończy zdania.
     */
    private static final Set<String> SHORTS = Set.of("ul", "al", "pl", "nr", "bud", "ok");

    /**
     * Uwagi bez spacji na brzegach.
     */
    private final String text;

    /**
     * Ctor.
     *
     * @param text Treść pola uwag bez spacji na brzegach, może być pusta
     */
    EntranceNote(final String text) {
        this.text = text;
    }

    /**
     * Czy uwagi opisują wyjście ewakuacyjne.
     *
     * @return Prawda, gdy uwagi zaczynają się od jednego z dwóch wzorów wyjścia
     */
    boolean exit() {
        final String lower = this.text.toLowerCase(Locale.ROOT);
        return EntranceNote.EXITS.stream().anyMatch(lower::startsWith);
    }

    /**
     * Nazwa wpisu.
     *
     * @return Pierwsze zdanie, gdy jest nazwą, „Wyjście ewakuacyjne” albo null bez nazwy
     */
    @Nullable String name() {
        final @Nullable String result;
        if (this.exit()) {
            result = EntranceNote.EXIT;
        } else if (this.named()) {
            result = this.first();
        } else {
            result = null;
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
        final Matcher name = EntranceNote.NAME.matcher(this.first());
        final boolean result;
        if (name.matches()) {
            final String last = name.group(1);
            result = last == null || !EntranceNote.SHORTS.contains(last.toLowerCase(Locale.ROOT));
        } else {
            result = false;
        }
        return result;
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
