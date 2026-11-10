/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

/**
 * Modele widoku V4 podane z zewnątrz, np. w testach; brakujący model ekran bierze z grafu Hilt dopiero w kroku,
 * który go potrzebuje.
 *
 * @property photoTranscoder przekodowanie zdjęcia w kroku 1, które przetrwa obrót ekranu (NFR-22)
 * @property entrances wejście z „Zgłoś barierę przy wejściu” do kroku 1 (US4.6)
 * @property send pozycja i zapis w kroku 3 (US4.1, NFR-23)
 */
data class QuickReportModels(
    val photoTranscoder: PhotoTranscoder? = null,
    val entrances: QuickEntranceViewModel? = null,
    val send: QuickSendViewModel? = null,
)
