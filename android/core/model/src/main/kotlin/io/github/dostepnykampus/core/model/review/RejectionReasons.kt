/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.review

/**
 * Powody odrzucenia, które Weryfikator wysyła, a aplikacja autora rozpoznaje (`rejectionReason` z kontraktu API).
 *
 * Wymagania: BR-23, BR-24
 */
public object RejectionReasons {
    /** Powód „nieaktualne”: zgłoszenie opisuje stan, który już się zmienił. */
    public const val OUTDATED: String = "nieaktualne"

    /**
     * Czy [reason] to [OUTDATED], bez względu na wielkość liter i odstępy na końcach.
     *
     * @param reason powód odrzucenia; `null`, gdy zgłoszenia nie odrzucono
     */
    public fun isOutdated(reason: String?): Boolean = reason?.trim().equals(OUTDATED, ignoreCase = true)
}
