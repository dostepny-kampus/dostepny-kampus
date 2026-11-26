/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.annotation.StringRes
import io.github.dostepnykampus.core.model.StaffRole

/**
 * Czynność roli pracowniczej na L2; widzi ją tylko osoba z rolą [role].
 *
 * Wymagania: US14.1, BR-1
 *
 * @property role rola, do której należy czynność
 * @property label napis wiersza
 */
enum class WorkAction(
    val role: StaffRole,
    @param:StringRes val label: Int,
) {
    /** Pomiar pozycji C1. Wymagania: US6 */
    MEASURE(StaffRole.COLLECTOR, R.string.konto_zmierz),

    /** Lista własnych zgłoszeń C3. Wymagania: US8 */
    MINE(StaffRole.COLLECTOR, R.string.konto_moje_zgloszenia),

    /** Postęp inwentaryzacji K1. Wymagania: US11 */
    PROGRESS(StaffRole.COORDINATOR, R.string.konto_postep),

    /** Kolejka Weryfikatora R1. Wymagania: US10.4 */
    REVIEW(StaffRole.VERIFIER, R.string.konto_sprawdz),

    /** Zaproszenie osoby K2. Wymagania: US12.1 */
    INVITE(StaffRole.COORDINATOR, R.string.konto_zapros),

    /** Eksport K3. Wymagania: US13 */
    EXPORT(StaffRole.COORDINATOR, R.string.konto_eksport),
}

/**
 * Czynności L2, które prowadzą poza ekran.
 *
 * @property onBack „Wstecz”
 * @property onWork wybrana czynność roli
 * @property onSignOut wylogowanie
 */
data class AccountActions(val onBack: () -> Unit, val onWork: (WorkAction) -> Unit, val onSignOut: () -> Unit)
