/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/**
 * Ekran zasłaniania: napis i „Zapisz zdjęcie”, przyciski zaznaczonej zasłony, zapisywanie, brak sieci przy
 * zapisie, wczytywanie i stany wspólne.
 *
 * Wymagania: BR-15, NFR-17, NFR-25, NFR-53
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class CoverScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val shown = mutableStateOf<CoverUiState>(CoverUiState.Loading)
    private val done = mutableListOf<String>()

    @Test
    fun savesPhoto() {
        show(READY)
        compose.onNodeWithText("Zasłoń twarz lub tablicę").assertIsDisplayed()
        compose.onNodeWithText("Dotknij zdjęcia w miejscu twarzy albo tablicy.").assertIsDisplayed()
        val buttons = compose.onAllNodes(hasText("Większy")).fetchSemanticsNodes()
        assertEquals("bez zaznaczenia bez przycisków zasłony", 0, buttons.size)
        compose.onNodeWithText("Zapisz zdjęcie").performClick()
        assertEquals("zapis", listOf("zapisz"), done)
    }

    @Test
    fun changesSelectedCover() {
        show(READY.copy(covers = Covers().tap(x = 0.2f, y = 0.2f).tap(x = 0.8f, y = 0.8f)))
        compose.onNodeWithText("Zasłona 2 z 2: zaznaczona").performScrollTo().assertIsDisplayed()
        listOf("Większy", "Mniejszy", "Usuń zasłonę").forEach { text ->
            compose.onNodeWithText(text).performScrollTo().performClick()
        }
        assertEquals("przyciski zaznaczonej zasłony", listOf("większy", "mniejszy", "usuń"), done)
    }

    @Test
    fun showsSavingAndOffline() {
        show(READY.copy(isSaving = true))
        compose.onNodeWithText("Zapisujemy zdjęcie…").assertIsDisplayed()
        shown.value = READY.copy(isOffline = true)
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
        val message = "Zapisanie zdjęcia wymaga internetu. Połącz się z internetem i zapisz ponownie."
        compose.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun loadsAndShowsCommonStates() {
        show(CoverUiState.Loading)
        compose.onNodeWithText("Wczytujemy zdjęcie…").assertIsDisplayed()
        shown.value = CoverUiState.Offline
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        shown.value = CoverUiState.Forbidden
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("ponowienie i powrót", listOf("ponów", "wstecz"), done)
    }

    private fun show(state: CoverUiState) {
        shown.value = state
        compose.setContent {
            DostepnyKampusTheme {
                CoverContent(
                    state = shown.value,
                    actions = coverActions { done += it },
                )
            }
        }
    }

    private companion object {
        val READY = CoverUiState.Ready(photo = CoverPhoto(grayJpeg()))
    }
}

private const val SIDE = 40
private const val QUALITY = 90

/** Szare zdjęcie JPEG do testów ekranu zasłaniania. */
internal fun grayJpeg(): ByteArray {
    val bitmap = Bitmap.createBitmap(SIDE, SIDE, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(Color.GRAY)
    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
        out.toByteArray()
    }
}

/** Czynności ekranu zasłaniania, które zapisują nazwę każdej czynności w [record]. */
internal fun coverActions(record: (String) -> Unit = {}): CoverActions =
    CoverActions(
        onBack = { record("wstecz") },
        onTap = { _, _ -> record("dotknięcie") },
        onBigger = { record("większy") },
        onSmaller = { record("mniejszy") },
        onRemove = { record("usuń") },
        onSave = { record("zapisz") },
        onRetry = { record("ponów") },
    )
