/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.review

import io.github.dostepnykampus.core.model.submission.Submission

/**
 * Decyzja Weryfikatora o wersji [version] zgłoszenia (`ReviewDecision` z kontraktu API).
 *
 * Wymagania: US10.2, US10.3, BR-3, BR-16
 */
public sealed interface ReviewDecision {
    /** Wersja zgłoszenia, którą ocenił Weryfikator. */
    public val version: Int

    /**
     * Przyjęcie zgłoszenia z typem i uciążliwością ustalonymi przez autora.
     *
     * Wymagania: US10.4
     *
     * @property version wersja ocenionego zgłoszenia
     */
    public data class Accept(override val version: Int) : ReviewDecision

    /**
     * Przyjęcie zgłoszenia szybkiego z typem i uciążliwością ustalonymi przez Weryfikatora.
     *
     * Wymagania: US10.3, BR-14
     *
     * @property version wersja ocenionego zgłoszenia
     * @property barrierType typ bariery, np. `TB-3`
     * @property severity uciążliwość 1–3
     */
    public data class AcceptQuick(override val version: Int, val barrierType: String, val severity: Int) :
        ReviewDecision

    /**
     * Odrzucenie zgłoszenia z powodem widocznym dla autora.
     *
     * Wymagania: US10.2, BR-16
     *
     * @property version wersja ocenionego zgłoszenia
     * @property reason powód odrzucenia, od 1 do 500 znaków
     */
    public data class Reject(override val version: Int, val reason: String) : ReviewDecision

    /**
     * Odrzucenie duplikatu ze wskazaniem wcześniejszego zgłoszenia.
     *
     * Wymagania: US10.2, BR-13, BR-16
     *
     * @property version wersja ocenionego zgłoszenia
     * @property reason powód odrzucenia, od 1 do 500 znaków
     * @property duplicateOf identyfikator wcześniejszego zgłoszenia
     */
    public data class RejectDuplicate(override val version: Int, val reason: String, val duplicateOf: String) :
        ReviewDecision
}

/**
 * Wynik wzięcia zgłoszenia do sprawdzenia.
 *
 * TODO(#3796): wynik trasy wzięcia w kliencie HTTP sprawdzania.
 *
 * Wymagania: US10.4, BR-11
 */
public sealed interface ReviewClaim {
    /**
     * Zgłoszenie jest w weryfikacji u tego Weryfikatora.
     *
     * @property submission zgłoszenie po wzięciu
     */
    public data class Claimed(val submission: Submission) : ReviewClaim

    /** Zgłoszenia nie da się wziąć: wziął je inny Weryfikator albo serwer nie ma jeszcze wszystkich zdjęć. */
    public data object Taken : ReviewClaim
}

/**
 * Wynik decyzji o zgłoszeniu.
 *
 * TODO(#3796): wynik trasy decyzji w kliencie HTTP sprawdzania.
 *
 * Wymagania: US10.8, BR-3
 */
public sealed interface ReviewOutcome {
    /**
     * Serwer zapisał decyzję.
     *
     * @property submission zgłoszenie po decyzji
     */
    public data class Decided(val submission: Submission) : ReviewOutcome

    /**
     * Decyzja nie zapadła: zgłoszenie ma inną wersję, jest już rozstrzygnięte albo brakuje jego zdjęcia.
     *
     * @property server zgłoszenie w wersji z serwera
     */
    public data class Changed(val server: Submission) : ReviewOutcome
}

/**
 * Zgłoszenie w pobliżu sprawdzanego.
 *
 * Wymagania: US10.1, BR-13
 *
 * @property submission podobne zgłoszenie
 * @property distanceMeters odległość od sprawdzanego zgłoszenia w metrach, od 0 do 10
 */
public data class SimilarSubmission(val submission: Submission, val distanceMeters: Double)
