/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.marking.barrierPlace
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Miejsce bariery słowami do arkusza bariery na V1 i do C7. Wymagania: US1.5 */
@RunWith(RobolectricTestRunner::class)
class BarrierPlaceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: miejsce to budynek z nazwą, wejście i piętro. Wymagania: US1.5 */
    @Test
    fun namesBuildingEntranceFloor() {
        show(symbol = "A-1", name = "Gmach Główny", entrance = "Wejście Cesarskie", floor = SubmissionFloor(number = 0))

        compose.onNodeWithText("A-1 Gmach Główny · Wejście Cesarskie, parter").assertExists()
    }

    /** Wiersz: budynek bez nazwy to „Budynek C-13”, piętro z dopiskiem „półpiętro”. Wymagania: US1.5 */
    @Test
    fun namesMezzanineWithoutName() {
        show(symbol = "C-13", name = null, entrance = null, floor = SubmissionFloor(number = 2, isMezzanine = true))

        compose.onNodeWithText("Budynek C-13 · piętro 2, półpiętro").assertExists()
    }

    /** Wiersz: bariera poza budynkiem ma „Poza budynkiem”. Wymagania: US1.5, BR-G2 */
    @Test
    fun namesOutside() {
        show(symbol = null, name = null, entrance = null, floor = null)

        compose.onNodeWithText("Poza budynkiem").assertExists()
    }

    private fun show(symbol: String?, name: String?, entrance: String?, floor: SubmissionFloor?) {
        compose.setContent { Text(barrierPlace(symbol = symbol, name = name, entrance = entrance, floor = floor)) }
    }
}
