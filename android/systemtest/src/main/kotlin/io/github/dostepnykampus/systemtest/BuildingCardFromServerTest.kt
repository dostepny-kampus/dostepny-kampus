/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.systemtest

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dostepnykampus.app.MainActivity
import io.github.dostepnykampus.core.testing.DeviceSteps
import io.github.dostepnykampus.core.testing.openedCard
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val WAIT_MS = 30_000L
private const val SERVER_ASSESSMENT = "Z map i opisów, stan z "

/**
 * Aplikacja z własnym grafem Hilt i siecią na emulatorze, serwer z bazą na komputerze gospodarza: katalog budynków
 * i karta budynku idą z serwera pod adresem z `API_BASE_URL`. Dzień i źródło oznaczenia wejść ma tylko odpowiedź
 * serwera, nie plik konturów w aplikacji.
 *
 * Test wykonuje ze scenariusza demo tylko krok karty budynku A-1 z serwera. Nie wykonuje kroków bez ekranu albo
 * przepływu w aplikacji: zapisu zgłoszenia TB-1 bez sieci, wysyłki po powrocie sieci, przyjęcia przez Weryfikatora,
 * postępu pięter i eksportu GeoJSON.
 *
 * Wymagania: US17.3, NFR-41
 */
@RunWith(AndroidJUnit4::class)
class BuildingCardFromServerTest {
    /** Prawdziwa aktywność aplikacji, uruchamiana od nowa dla każdego testu. */
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private val steps = DeviceSteps(compose, WAIT_MS)

    /** Zamyka instrukcję pierwszego uruchomienia, która jest na czystych danych aplikacji. Wymagania: US5.1 */
    @Before
    fun leaveHelp() {
        steps.leaveHelp()
    }

    /**
     * Scenariusz (częściowo): oznaczenie-wejscia.feature: Wspólna data i źródło stoją raz pod listą wejść.
     * Scenariusz (częściowo): scenariusz-demo.feature: Schody przy wejściu głównym A-1 od pomiaru do eksportu
     *
     * Wymagania: US17.3, US1.1, US2.8
     */
    @Test
    fun cardA1FromServer() {
        steps.openCard("A-1")
        compose.onNode(openedCard()).performClick()
        val assessment = hasText(SERVER_ASSESSMENT, substring = true)
        steps.waitFor(assessment)
        compose.onAllNodes(assessment).assertCountEquals(1)
    }
}
