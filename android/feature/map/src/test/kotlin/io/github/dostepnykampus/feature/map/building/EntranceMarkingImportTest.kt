/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.marking.MarkingSign
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.abs

private const val SIGN_TAG = "znak-oznaczenia"
private const val IMPORT_ROW = "Z map i opisów, stan z 3.11.2026"
private const val INACCESSIBLE_WORD = "Niedostępne"
private const val ACCESSIBLE_WORD = "Dostępne"
private const val NO_DATA_WORD = "Brak danych"
private const val MIN_DIFFERING_PIXELS = 100
private const val LUMA_DIFFERENCE = 64

/** Oznaczenie wejścia z importu danych PWr na karcie budynku, także bez danych. Wymagania: US17.1, US17.3 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EntranceMarkingImportTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Import z danych PWr nadaje wejściu głównemu oznaczenie „niedostępne”.
     * Scenariusz: oznaczenie-wejscia.feature: Import nadaje wejściu oznaczenie z danych PWr
     *
     * Wymagania: US17.1, US17.3, US2.1, BR-24
     */
    @Test
    fun importMarksMainInaccessible() {
        showCard(
            symbol = "A-1",
            entrances =
                listOf(
                    entrance(
                        symbol = "A-1",
                        name = "Wejście główne",
                        marking = EntranceMarking.INACCESSIBLE,
                        isMain = true,
                    ),
                ),
        )
        assertEquals(
            "napis „$INACCESSIBLE_WORD” przy wejściu głównym A-1",
            1,
            countOf(INACCESSIBLE_WORD),
        )
        assertSignAndImportRow()
    }

    /**
     * Import z danych PWr nadaje innemu wejściu oznaczenie „dostępne”.
     * Scenariusz: oznaczenie-wejscia.feature: Import nadaje wejściu oznaczenie z danych PWr
     *
     * Wymagania: US17.1, US17.3, US2.1, BR-24
     */
    @Test
    fun importMarksOtherAccessible() {
        showCard(
            symbol = "A-1",
            entrances =
                listOf(
                    entrance(
                        symbol = "A-1",
                        name = "Inne wejście",
                        marking = EntranceMarking.ACCESSIBLE,
                        isMain = false,
                    ),
                ),
        )
        assertEquals("napis „$ACCESSIBLE_WORD” przy innym wejściu A-1", 1, countOf(ACCESSIBLE_WORD))
        assertSignAndImportRow()
    }

    /**
     * Import bez danych daje wejściu oznaczenie „brak danych”, nie „niedostępne”.
     * Scenariusz: oznaczenie-wejscia.feature: Import bez danych daje oznaczenie „brak danych”
     *
     * Wymagania: US17.1, US17.3, US2.1, BR-24
     */
    @Test
    fun importWithoutDataNoData() {
        showCard(
            symbol = "B-8",
            entrances =
                listOf(
                    entrance(
                        symbol = "B-8",
                        name = "Wejście boczne",
                        marking = EntranceMarking.NO_DATA,
                        isMain = false,
                    ),
                ),
        )
        assertEquals("napis „$NO_DATA_WORD” przy wejściu bocznym B-8", 1, countOf(NO_DATA_WORD))
        assertEquals("wejście boczne B-8 bez napisu „$INACCESSIBLE_WORD”", 0, countOf(INACCESSIBLE_WORD))
        assertSignAndImportRow()
    }

    /**
     * Trzy oznaczenia da się odróżnić w odcieniach szarości, bo każde ma inny kształt albo ton.
     * Scenariusz: oznaczenie-wejscia.feature: Import bez danych daje oznaczenie „brak danych”
     *
     * Wymagania: US2.1, NFR-21
     */
    @Test
    fun markingsDifferInGrayscale() {
        compose.setContent {
            DostepnyKampusTheme {
                Column { EntranceMarking.entries.forEach { MarkingSign(marking = it) } }
            }
        }
        val signs = compose.onAllNodesWithTag(SIGN_TAG, useUnmergedTree = true)
        signs.assertCountEquals(EntranceMarking.entries.size)
        val lumas =
            EntranceMarking.entries.indices.map { index -> luma(signs[index].captureToImage().asAndroidBitmap()) }
        EntranceMarking.entries.indices.forEach { first ->
            (first + 1 until EntranceMarking.entries.size).forEach { second ->
                val differing = differingPixels(first = lumas[first], second = lumas[second])
                assertTrue(
                    "${EntranceMarking.entries[first]} i ${EntranceMarking.entries[second]} w szarości: $differing px",
                    differing >= MIN_DIFFERING_PIXELS,
                )
            }
        }
    }

    private fun assertSignAndImportRow() {
        val signs = compose.onAllNodesWithTag(SIGN_TAG, useUnmergedTree = true).fetchSemanticsNodes().size
        assertTrue("ikona oznaczenia przy wejściu", signs > 0)
        assertTrue(
            "wiersz daty i źródła „$IMPORT_ROW” z importu",
            compose.onAllNodes(hasText(IMPORT_ROW, substring = true)).fetchSemanticsNodes().isNotEmpty(),
        )
    }

    private fun luma(bitmap: Bitmap): IntArray =
        IntArray(bitmap.width * bitmap.height) { index ->
            val pixel = bitmap.getPixel(index % bitmap.width, index / bitmap.width)
            (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)).toInt()
        }

    private fun differingPixels(first: IntArray, second: IntArray): Int =
        first.indices.count { index -> abs(first[index] - second[index]) > LUMA_DIFFERENCE }

    private fun countOf(text: String): Int = compose.onAllNodes(hasText(text)).fetchSemanticsNodes().size

    private fun showCard(symbol: String, entrances: List<Entrance>) {
        val viewModel =
            BuildingCardViewModel(
                symbol = symbol,
                repository = catalog(symbol),
                networkStatus = { flowOf(true) },
                serverEntrances = ServerEntrances { entrances },
            )
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardScreen(
                    symbol = symbol,
                    mode = CardMode.Expanded,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    viewModel = viewModel,
                )
            }
        }
        val first = entrances.first().name.orEmpty()
        compose.waitUntil { compose.onAllNodes(hasText(first, substring = true)).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun entrance(symbol: String, name: String, marking: EntranceMarking, isMain: Boolean) =
        Entrance(
            id = name,
            buildingSymbol = symbol,
            name = name,
            isMain = isMain,
            type = null,
            location = GeoPoint(longitude = 0.0, latitude = 0.0),
            marking = marking,
            assessment = EntranceAssessment(IMPORT_DAY, AssessmentSource.IMPORT),
        )

    private companion object {
        val IMPORT_DAY: LocalDate = LocalDate.of(2026, 11, 3)

        fun catalog(symbol: String) =
            BuildingCatalogRepository(EmptyCoroutineContext) {
                """{"type":"FeatureCollection","attribution":"©","features":[""" +
                    """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"osm_id":"way/1"},""" +
                    """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
            }
    }
}
