/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.core.model.submission.BarrierGroup
import kotlinx.serialization.Serializable

/**
 * V4 Zgłoszenie szybkie na kroku [step]. Każdy krok to osobny wpis historii, więc „Wstecz” cofa o krok.
 * [entranceId]: wejście wybrane w arkuszu wejścia przez „Zgłoś barierę przy wejściu”, a [place]: obiekt
 * wybrany w arkuszu znaku planu. Wpis niesie
 * dane z wcześniejszych kroków, więc powrót między krokami nie usuwa zdjęcia ani grupy.
 *
 * Wymagania: US4, NFR-15, US4.6, US4.8, BR-14, US4.5, US4.2
 *
 * @property step bieżący krok
 * @property entranceId wejście z arkusza wejścia; `null`, gdy zgłoszenie nie wskazuje wejścia
 * @property place obiekt z arkusza znaku planu; `null`, gdy zgłoszenie nie wskazuje obiektu
 * @property photoId identyfikator zdjęcia z kroku 1
 * @property photoPath plik zdjęcia z kroku 1
 * @property group grupa barier z kroku 2
 * @property floorNumber piętro z kroku 2; `null` to „Nie wiem” albo brak pytania
 * @property floorId piętro z danych PWr w budynku szczegółowym
 * @property floorLabel etykieta piętra [floorId]
 * @property isFloorAsked czy krok 2 pytał o piętro
 * @property isSavedOffline potwierdzenie po zapisie bez internetu
 */
@Serializable
data class ReportRoute(
    val step: ReportStep = ReportStep.PHOTO,
    val entranceId: String? = null,
    val place: ReportPlace? = null,
    val photoId: String? = null,
    val photoPath: String? = null,
    val group: BarrierGroup? = null,
    val floorNumber: Int? = null,
    val floorId: String? = null,
    val floorLabel: String? = null,
    val isFloorAsked: Boolean = false,
    val isSavedOffline: Boolean = false,
) : NavKey

/** Krok V4. */
enum class ReportStep {
    /** Krok 1: zdjęcie. Wymagania: US4.1 */
    PHOTO,

    /** Krok 2: grupa barier i piętro. Wymagania: US4.3, US4.5 */
    GROUP,

    /** Krok 3: wysłanie. Wymagania: US4.1 */
    SEND,

    /** Potwierdzenie po wysłaniu; kroki znikają wtedy z historii (P2). Wymagania: US4.4 */
    SENT,
}
