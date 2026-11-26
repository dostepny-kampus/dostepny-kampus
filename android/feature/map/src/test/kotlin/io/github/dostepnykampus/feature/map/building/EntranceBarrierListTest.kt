/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Bariera przy wejściu na V2: wiersz ze znakiem uciążliwości w wierszu wejścia, dopisek w zdaniu pod wierszem
 * o wejściach i brak wejścia dostępnego, gdy jedyne dostępne ma blokadę.
 *
 * Wymagania: US2.2, US3.1, BR-25
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntranceBarrierListTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, „Tylko z pomocą” typu „ciężkie drzwi” przy zalecanym „Wejście B”, jedynym dostępnym → w wierszu
     *   wejścia „Tylko z pomocą: ciężkie drzwi”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Bariera przy wejściu zalecanym jest na karcie przy tym
     *   wejściu
     */
    @Test
    fun listsMinorBarrierInRow() {
        show(ONLY_B_ACCESSIBLE, HELP) { EntranceListContent(it) }
        compose.onNode(hasText("Tylko z pomocą: ciężkie drzwi"), useUnmergedTree = true).assertExists()
    }

    /**
     * Wiersz: V2, „Tylko z pomocą” przy zalecanym „Wejście B”, jedynym dostępnym, z drzwiami 100 cm → zdanie
     *   kończy się „Przy wejściu jest bariera: tylko z pomocą.”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Bariera przy wejściu zalecanym jest na karcie przy tym
     *   wejściu
     */
    @Test
    fun answersWithMinorBarrier() {
        show(ONLY_B_ACCESSIBLE, HELP) { CardHeaderContent(it) }
        compose
            .onNode(hasText("Zalecane: „Wejście B”. Drzwi 100 cm. Przy wejściu jest bariera: tylko z pomocą."))
            .assertExists()
    }

    /**
     * Wiersz: V2, blokada przy jedynym wejściu dostępnym → „Brak wejścia dostępnego” i „Jedyne wejście dostępne
     *   ma blokadę.”.
     * Scenariusz (częściowo, przykład: nie ma): przegladanie-mapy.feature: Wejście z blokadą nie jest
     *   zalecane, a jego oznaczenie zostaje
     */
    @Test
    fun answersBlockedOnlyAccessible() {
        show(ONLY_B_ACCESSIBLE, listOf(entranceBarrier("b", Severity.BLOCKING, BarrierType.STAIRS_AT_ENTRANCE))) {
            CardHeaderContent(it)
        }
        compose.onNode(hasText("Brak wejścia dostępnego"), useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Jedyne wejście dostępne ma blokadę."), useUnmergedTree = true).assertExists()
    }

    /** Zrzut listy wejść z blokadą przy jednym wejściu i barierą przy drugim, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        show(TWO_ENTRANCES, MIXED) { EntranceListContent(it) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut listy wejść z barierami przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        show(TWO_ENTRANCES, MIXED) { EntranceListContent(it) }
        compose.onRoot().captureRoboImage()
    }

    private fun show(
        entrances: List<Entrance>,
        barriers: List<AcceptedBarrier>,
        content: @Composable (BuildingCardUiState.Ready) -> Unit,
    ) {
        val card = readyCard(symbol = "C-13", entrances = entrances, barriers = barriers)
        compose.setContent { content(card) }
    }

    private companion object {
        /** „Wejście B” jako jedyne „dostępne”, „Wejście C” „niedostępne”. */
        val ONLY_B_ACCESSIBLE =
            listOf(sampleEntrance("b"), sampleEntrance("c").copy(marking = EntranceMarking.INACCESSIBLE))

        val HELP = listOf(entranceBarrier("b", Severity.ONLY_WITH_HELP))

        val MIXED =
            listOf(
                entranceBarrier("b", Severity.BLOCKING, BarrierType.STAIRS_AT_ENTRANCE),
                entranceBarrier("c", Severity.ONLY_WITH_HELP),
            )
    }
}
