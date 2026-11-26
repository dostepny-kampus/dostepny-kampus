/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
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
 * Zdjęcie do zasłaniania: dotknięcie we współrzędnych względnych, opis zasłon dla TalkBack z numerem
 * i położeniem słowami, zaznaczenie zasłony czynnością TalkBack.
 *
 * Wymagania: BR-15, NFR-18, NFR-53
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp")
class CoverPhotoViewTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val taps = mutableListOf<Pair<Float, Float>>()

    @Test
    fun reportsTapAsRelativePoint() {
        show(Covers())
        compose.onNodeWithContentDescription(PHOTO_TEXT).performTouchInput {
            click(Offset(x = width * QUARTER, y = height * HALF))
        }
        val (x, y) = taps.single()
        assertEquals("od lewej", QUARTER, x, TOLERANCE)
        assertEquals("od góry", HALF, y, TOLERANCE)
    }

    @Test
    fun readsCoversWithNumberAndPlace() {
        show(TWO)
        compose.onNodeWithContentDescription("Zasłona 1 z 2, u góry po lewej").assertExists()
        compose.onNodeWithContentDescription("Zasłona 2 z 2, na dole po prawej, zaznaczona").assertExists()
    }

    @Test
    fun selectsCoverWithTalkBack() {
        show(TWO)
        compose
            .onNodeWithContentDescription("Zasłona 1 z 2, u góry po lewej")
            .performSemanticsAction(SemanticsActions.OnClick)
        assertEquals("dotknięcie w środku zasłony", listOf(EDGE to EDGE), taps)
        compose.onNodeWithContentDescription(PHOTO_TEXT).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals("zasłona pośrodku zdjęcia", HALF to HALF, taps.last())
    }

    private fun show(covers: Covers) {
        compose.setContent {
            DostepnyKampusTheme {
                CoverPhotoView(
                    photo = PHOTO,
                    covers = covers,
                    onTap = { x, y -> taps += x to y },
                    modifier = Modifier.width(PHOTO_WIDTH.dp),
                )
            }
        }
        compose.waitUntilAtLeastOneExists(hasContentDescription(PHOTO_TEXT), timeoutMillis = WAIT)
    }

    private companion object {
        const val PHOTO_TEXT = "Zdjęcie do zasłonięcia"
        const val PHOTO_WIDTH = 300
        const val QUARTER = 0.25f
        const val HALF = 0.5f
        const val EDGE = 0.1f
        const val TOLERANCE = 0.01f
        const val WAIT = 5_000L
        const val SIDE = 400
        const val QUALITY = 90
        val TWO: Covers = Covers().tap(x = EDGE, y = EDGE).tap(x = 0.9f, y = 0.9f)
        val PHOTO: CoverPhoto = CoverPhoto(whiteJpeg())

        fun whiteJpeg(): ByteArray {
            val bitmap = Bitmap.createBitmap(SIDE, SIDE * 3 / 4, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            return ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                out.toByteArray()
            }
        }
    }
}
