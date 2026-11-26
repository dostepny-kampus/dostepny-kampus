/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.mine

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.data.submission.own.RefusalReason
import io.github.dostepnykampus.core.data.submission.own.SendState
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Instant
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SubmissionCardTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var opened = 0

    private fun show(item: OwnSubmissionItem, onOpen: (() -> Unit)? = { opened++ }) {
        compose.setContent {
            DostepnyKampusTheme {
                SubmissionCard(item = item, onOpen = onOpen, onMeasure = {}, onConflict = null)
            }
        }
    }

    /** „Otwórz” to czynność karty szeroka jak napis, węższa niż treść karty. Wymagania: NFR-17, US8.1 */
    @Test
    fun keepsOpenCompact() {
        show(item(id = "a", state = SendState.SENT))
        val bounds = compose.onNodeWithText("Otwórz").getUnclippedBoundsInRoot()
        assertTrue("„Otwórz” ma być węższe niż treść karty: $bounds", (bounds.right - bounds.left).value < CONTENT_DP)
    }

    /**
     * Wiersz: C3 zgłoszenie → status, typ, budynek z piętrem i data.
     * Scenariusz (częściowo, przykład: wysłane): zgloszenia-zbierajacego.feature:
     *   Każde własne zgłoszenie ma jeden status tekstem i ikoną
     *
     * Wymagania: US8.1
     */
    @Test
    fun showsItemDetails() {
        show(item(id = "a", state = SendState.SENT))
        compose.onNodeWithText("Wysłane").assertIsDisplayed()
        compose.onNodeWithText("Budynek A-1, Piętro 2").assertIsDisplayed()
        compose.onNodeWithText("9.11.2026").assertIsDisplayed()
    }

    /**
     * Wiersz: C3 po sprawdzeniu → status z serwera, przy odrzuconym powód; poprawka „W telefonie”.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Każde własne zgłoszenie ma jeden status
     *   tekstem i ikoną
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zgłoszenie odrzucone pokazuje powód odrzucenia
     *
     * Wymagania: US8.2, BR-9
     */
    @Test
    fun showsServerStatuses() {
        val words =
            mapOf(
                SubmissionStatus.SENT to "Wysłane",
                SubmissionStatus.IN_REVIEW to "W weryfikacji",
                SubmissionStatus.ACCEPTED to "Przyjęte",
                SubmissionStatus.ARCHIVED to "Archiwalne",
            )
        val shown = words.keys.map { item(id = "a", state = SendState.SENT).copy(serverStatus = it).shownStatus() }
        assertEquals(listOf(ShownStatus.SENT, ShownStatus.IN_REVIEW, ShownStatus.ACCEPTED, ShownStatus.ARCHIVED), shown)
        val local = item(id = "a", state = SendState.LOCAL).copy(serverStatus = SubmissionStatus.ACCEPTED)
        assertEquals(ShownStatus.LOCAL, local.shownStatus())
        val rejected = item(id = "a", state = SendState.SENT).copy(serverStatus = SubmissionStatus.REJECTED)
        show(rejected.copy(rejectionReason = "Zdjęcie nieostre"))
        compose.onNodeWithText("Odrzucone").assertIsDisplayed()
        compose.onNodeWithText("Powód odrzucenia: Zdjęcie nieostre").assertIsDisplayed()
    }

    /**
     * Każdy status ma widoczną ikonę, a ikony różnią się w odcieniach szarości, więc nie zależą od koloru.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Każde własne zgłoszenie ma jeden status
     *   tekstem i ikoną
     *
     * Wymagania: US8.1, NFR-21
     */
    @Test
    fun iconsDifferInGrayscale() {
        compose.setContent {
            DostepnyKampusTheme {
                Column {
                    ShownStatus.entries.forEach { status ->
                        Icon(
                            imageVector = status.icon,
                            contentDescription = null,
                            modifier = Modifier.size(ICON_DP).testTag(status.name),
                        )
                    }
                }
            }
        }
        val grays =
            ShownStatus.entries.map { status ->
                grayOnWhite(compose.onNodeWithTag(status.name).captureToImage().asAndroidBitmap())
            }
        grays.forEachIndexed { index, gray ->
            val dark = gray.count { it < DARK_LUMA }
            assertTrue("ikona „${ShownStatus.entries[index]}” ma widoczne piksele: $dark", dark >= MIN_PIXELS)
        }
        grays.indices.forEach { first ->
            (first + 1 until grays.size).forEach { second ->
                val differing = differingPixels(first = grays[first], second = grays[second])
                assertTrue(
                    "${ShownStatus.entries[first]} i ${ShownStatus.entries[second]} w szarości: $differing px",
                    differing >= MIN_PIXELS,
                )
            }
        }
    }

    /**
     * Karta z jednym statusem: słowo i ikona obok, inne statusy nie są widoczne, a ikony różnią się w szarości.
     * Scenariusz: zgloszenia-zbierajacego.feature: Każde własne zgłoszenie ma jeden status tekstem i ikoną
     *
     * Wymagania: US8.1, NFR-21
     */
    @Test
    fun showsOneStatusWithIconPerRow() {
        val rows =
            listOf(
                item(id = "a", state = SendState.LOCAL) to "W telefonie",
                item(id = "b", state = SendState.SENT, serverStatus = SubmissionStatus.SENT) to "Wysłane",
                item(id = "c", state = SendState.SENT, serverStatus = SubmissionStatus.IN_REVIEW) to "W weryfikacji",
                item(id = "d", state = SendState.SENT, serverStatus = SubmissionStatus.ACCEPTED) to "Przyjęte",
                item(id = "e", state = SendState.SENT, serverStatus = SubmissionStatus.REJECTED) to "Odrzucone",
                item(id = "f", state = SendState.SENT, serverStatus = SubmissionStatus.ARCHIVED) to "Archiwalne",
            )
        var shown by mutableStateOf(rows.first().first)
        compose.setContent {
            DostepnyKampusTheme {
                SubmissionCard(item = shown, onOpen = null, onMeasure = {}, onConflict = null)
            }
        }
        val grays =
            rows.map { (candidate, word) ->
                shown = candidate
                compose.onNodeWithText(word).assertIsDisplayed()
                rows.map { it.second }.filter { it != word }.forEach { other ->
                    val count = compose.onAllNodesWithText(other).fetchSemanticsNodes().size
                    assertEquals("karta „$word” nie pokazuje statusu „$other”", 0, count)
                }
                grayIconBeside(word)
            }
        grays.forEachIndexed { index, gray ->
            val dark = gray.count { it < DARK_LUMA }
            assertTrue("ikona przy „${rows[index].second}” ma widoczne piksele: $dark", dark >= MIN_PIXELS)
        }
        grays.indices.forEach { first ->
            (first + 1 until grays.size).forEach { second ->
                val differing = differingPixels(first = grays[first], second = grays[second])
                assertTrue(
                    "ikony „${rows[first].second}” i „${rows[second].second}” w szarości: $differing px",
                    differing >= MIN_PIXELS,
                )
            }
        }
    }

    /**
     * Scenariusz: zgloszenia-zbierajacego.feature: Zgłoszenie ze zmianą czekającą w telefonie ma napis
     *   przy statusie.
     */
    @Test
    fun marksWaitingChange() {
        show(item(id = "a", state = SendState.SENT).copy(isChangeWaiting = true))
        compose.onNodeWithText("Wysłane").assertIsDisplayed()
        compose.onNodeWithText("Zmiana czeka na wysłanie").assertIsDisplayed()
    }

    @Test
    fun skipsMarkWithoutChange() {
        show(item(id = "a", state = SendState.SENT))
        compose.onNodeWithText("Zmiana czeka na wysłanie").assertDoesNotExist()
    }

    /** Wiersz: C3 „Nie wysłano” → powód odmowy. Wymagania: US9.5, BR-26 */
    @Test
    fun showsRefusalReason() {
        show(item(id = "a", state = SendState.REFUSED, refusal = RefusalReason.NO_PERMISSION))
        compose.onNodeWithText("Nie wysłano").assertIsDisplayed()
        val reason = "Nie masz już uprawnień do tej czynności. Jeśli to pomyłka, poproś Koordynatora o zaproszenie."
        compose.onNodeWithText(reason).assertIsDisplayed()
    }

    /** Wiersz: C3 „Otwórz” → poprawka w C2. Wymagania: US8.3 */
    @Test
    fun opensItem() {
        show(item(id = "a", state = SendState.LOCAL))
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals(1, opened)
    }

    /** Wiersz: C3 bez poprawki w C2 → pozycja bez „Otwórz”. Wymagania: US8.3 */
    @Test
    fun hidesOpenWithoutEdit() {
        show(item(id = "a", state = SendState.LOCAL), onOpen = null)
        compose.onNodeWithText("Otwórz").assertDoesNotExist()
    }

    /**
     * Wiersz: C3 „Otwórz” tylko przy zgłoszeniach, które można poprawić; przyjęte i archiwalne bez „Otwórz”.
     * Scenariusz: zgloszenia-zbierajacego.feature: „Otwórz” mają tylko zgłoszenia, które można poprawić
     *
     * Wymagania: US8.1, US8.3
     */
    @Test
    fun opensOnlyEditable() {
        val cases =
            listOf(
                Triple(item("a", SendState.LOCAL), "W telefonie", true),
                Triple(item("b", SendState.REFUSED, RefusalReason.NOT_ACCEPTED), "Nie wysłano", true),
                Triple(item("c", SendState.SENT, serverStatus = SubmissionStatus.SENT), "Wysłane", true),
                Triple(item("d", SendState.SENT, serverStatus = SubmissionStatus.IN_REVIEW), "W weryfikacji", true),
                Triple(item("e", SendState.SENT, serverStatus = SubmissionStatus.REJECTED), "Odrzucone", true),
                Triple(item("f", SendState.SENT, serverStatus = SubmissionStatus.ACCEPTED), "Przyjęte", false),
                Triple(item("g", SendState.SENT, serverStatus = SubmissionStatus.ARCHIVED), "Archiwalne", false),
            )
        var shown by mutableStateOf(cases.first().first)
        compose.setContent {
            DostepnyKampusTheme {
                SubmissionCard(item = shown, onOpen = { opened++ }, onMeasure = {}, onConflict = null)
            }
        }
        var expectedClicks = 0
        cases.forEach { (candidate, statusText, expectsOpen) ->
            shown = candidate
            compose.onNodeWithText(statusText).assertIsDisplayed()
            if (expectsOpen) {
                compose.onNodeWithText("Otwórz").assertIsDisplayed()
                compose.onNodeWithText("Otwórz").performClick()
                expectedClicks++
            } else {
                compose.onNodeWithText("Otwórz").assertDoesNotExist()
            }
            assertEquals(expectedClicks, opened)
        }
    }

    /** Wiersz: C3 poza budynkiem, nieznany kod typu → „Poza budynkiem”, „Nieznany typ bariery”. Wymagania: US8.1 */
    @Test
    fun showsOutsideUnknownType() {
        show(item(id = "a", state = SendState.SENT).copy(type = null, buildingSymbol = null, floorNumber = null))
        compose.onNodeWithText("Poza budynkiem").assertIsDisplayed()
        compose.onNodeWithText("Nieznany typ bariery").assertIsDisplayed()
    }

    /** Wiersz: C3 budynek bez numeru piętra → sam budynek. Wymagania: US8.1 */
    @Test
    fun showsBuildingWithoutFloor() {
        show(item(id = "a", state = SendState.SENT).copy(floorNumber = null))
        compose.onNodeWithText("Budynek A-1").assertIsDisplayed()
    }

    /** Wiersz: C3 → TalkBack czyta status przed typem bariery. Wymagania: NFR-18 */
    @Test
    fun readsStatusBeforeType() {
        show(item(id = "a", state = SendState.LOCAL))
        val tree = compose.onRoot(useUnmergedTree = false).printToString()
        assertTrue(tree.indexOf("W telefonie") in 0 until tree.indexOf("Schody przy wejściu"))
    }

    private fun grayIconBeside(word: String): IntArray {
        val text = compose.onNodeWithText(word, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        val side = STATUS_ICON_DP.value * density
        val right = text.left - Odstepy.s.value * density
        val top = text.center.y - side / 2
        val root = compose.onRoot().captureToImage().asAndroidBitmap()
        val icon = Bitmap.createBitmap(root, (right - side).toInt(), top.toInt(), side.toInt(), side.toInt())
        return grayOnWhite(icon)
    }

    private fun grayOnWhite(bitmap: Bitmap): IntArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return IntArray(pixels.size) { index ->
            val pixel = pixels[index]
            val alpha = Color.alpha(pixel) / OPAQUE
            val luma = 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
            (alpha * luma + (1 - alpha) * OPAQUE).toInt()
        }
    }

    private fun differingPixels(first: IntArray, second: IntArray): Int =
        first.indices.count { index -> abs(first[index] - second[index]) > LUMA_DIFFERENCE }
}

private val ICON_DP = 48.dp
private val STATUS_ICON_DP = 24.dp
private const val CONTENT_DP = 328f
private const val DARK_LUMA = 128
private const val LUMA_DIFFERENCE = 64
private const val MIN_PIXELS = 100
private const val OPAQUE = 255.0

private fun item(
    id: String,
    state: SendState,
    refusal: RefusalReason? = null,
    serverStatus: SubmissionStatus? = null,
) = OwnSubmissionItem(
    id = id,
    savedAt = Instant.parse("2026-11-09T12:00:00Z"),
    type = BarrierType.STAIRS_AT_ENTRANCE,
    buildingSymbol = "A-1",
    floorNumber = 2,
    state = state,
    refusal = refusal,
    serverStatus = serverStatus,
)
