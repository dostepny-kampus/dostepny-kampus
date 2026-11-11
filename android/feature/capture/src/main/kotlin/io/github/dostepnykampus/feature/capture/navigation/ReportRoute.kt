/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.navigation

import androidx.navigation3.runtime.NavKey
import io.github.dostepnykampus.core.model.BarrierGroup
import kotlinx.serialization.Serializable

/**
 * V4 Zgłoszenie szybkie (US4) na kroku [step]. Każdy krok to osobny wpis historii, więc „Wstecz” cofa o krok
 * (NFR-15). [entranceId]: wejście wybrane na V2 albo V3 przez „Zgłoś barierę przy wejściu” (US4.6). Wpis niesie
 * dane z wcześniejszych kroków, więc powrót między krokami nie usuwa zdjęcia ani grupy (US4.8).
 *
 * @property step bieżący krok
 * @property entranceId wejście z V2 albo V3; `null`, gdy zgłoszenie zaczęło się z mapy
 * @property photoId identyfikator zdjęcia z kroku 1
 * @property photoPath plik zdjęcia z kroku 1
 * @property group grupa barier z kroku 2
 * @property floorNumber piętro z kroku 2; `null` to „Nie wiem” albo brak pytania (BR-14)
 * @property floorId piętro z danych PWr w budynku szczegółowym (US4.5)
 * @property floorLabel etykieta piętra [floorId]
 * @property isFloorAsked czy krok 2 pytał o piętro
 * @property isSavedOffline potwierdzenie po zapisie bez internetu (US4.2)
 */
@Serializable
data class ReportRoute(
    val step: ReportStep = ReportStep.PHOTO,
    val entranceId: String? = null,
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
    /** Krok 1: zdjęcie (US4.1). */
    PHOTO,

    /** Krok 2: grupa barier i piętro (US4.3, US4.5). */
    GROUP,

    /** Krok 3: wysłanie (US4.1). */
    SEND,

    /** Potwierdzenie po wysłaniu; kroki znikają wtedy z historii (US4.4, P2). */
    SENT,
}
