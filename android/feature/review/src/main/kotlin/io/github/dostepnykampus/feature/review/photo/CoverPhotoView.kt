/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.photo

import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.feature.review.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import io.github.dostepnykampus.core.designsystem.R as DesignR

private const val SHOWN_PIXELS = 2048
private const val CENTER = 0.5f

/**
 * Zdjęcie [photo] z zasłonami [covers]; dotknięcie zdjęcia albo zasłony zgłasza [onTap] we współrzędnych
 * względnych.
 *
 * Wymagania: BR-15, NFR-18, NFR-21, NFR-53
 *
 * @param photo zdjęcie z serwera
 * @param covers zasłony i zaznaczona zasłona
 * @param onTap dotknięcie we współrzędnych względnych od lewej i od góry
 * @param modifier wymiary nadane przez ekran
 * @param decodeContext kontekst dekodowania zdjęcia poza wątkiem interfejsu
 */
@Composable
internal fun CoverPhotoView(
    photo: CoverPhoto,
    covers: Covers,
    onTap: (x: Float, y: Float) -> Unit,
    modifier: Modifier = Modifier,
    decodeContext: CoroutineContext = decodeContext(),
) {
    val image by produceState<ImageBitmap?>(initialValue = null, photo) {
        value = withContext(decodeContext) { decodedForScreen(photo.jpeg) }
    }
    val tap by rememberUpdatedState(onTap)
    val shown = image
    val photoText = stringResource(R.string.zaslona_zdjecie)
    if (shown == null) {
        Text(
            text = stringResource(DesignR.string.zdjecie_wczytujemy),
            modifier = modifier,
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(shown.width.toFloat() / shown.height)
                .pointerInput(Unit) { detectTapGestures { at -> tap(at.x / size.width, at.y / size.height) } }
                .semantics {
                    contentDescription = photoText
                    onClick {
                        tap(CENTER, CENTER)
                        true
                    }
                },
    ) {
        Image(
            bitmap = shown,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
        covers.items.forEachIndexed { index, cover ->
            val place = stringResource(cover.place().nameRes())
            val isSelected = index == covers.selected
            val text = if (isSelected) R.string.zaslona_czytnik_zaznaczona else R.string.zaslona_czytnik
            CoverMark(
                cover = cover,
                description = stringResource(text, index + 1, covers.items.size, place),
                isSelected = isSelected,
                area = maxWidth to maxHeight,
                onSelect = { tap(cover.x, cover.y) },
            )
            if (isSelected) SelectedLabel(cover = cover, area = maxWidth to maxHeight)
        }
    }
}

@Composable
private fun CoverMark(
    cover: Cover,
    description: String,
    isSelected: Boolean,
    area: Pair<Dp, Dp>,
    onSelect: () -> Unit,
) {
    val (width, height) = area
    val half = cover.size / 2
    Box(
        modifier =
            Modifier
                .offset(x = width * (cover.x - half), y = height * (cover.y - half))
                .size(width = width * cover.size, height = height * cover.size)
                .background(Color.Black)
                .border(width = if (isSelected) Obramowania.pole else Obramowania.ramka, color = Color.White)
                .clearAndSetSemantics {
                    contentDescription = description
                    onClick {
                        onSelect()
                        true
                    }
                },
    )
}

@Composable
private fun SelectedLabel(cover: Cover, area: Pair<Dp, Dp>) {
    val (width, height) = area
    val shape = RoundedCornerShape(Zaokraglenia.srednie)
    Text(
        text = stringResource(R.string.zaslona_zaznaczona),
        modifier =
            Modifier
                .besideCover(cover = cover, width = width, height = height)
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(Obramowania.ramka, MaterialTheme.colorScheme.outline, shape)
                .padding(horizontal = Odstepy.s, vertical = Odstepy.xs)
                .semantics { hideFromAccessibility() },
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        style = MaterialTheme.typography.labelLarge,
    )
}

private fun Modifier.besideCover(cover: Cover, width: Dp, height: Dp): Modifier =
    layout { measurable, _ ->
        val label = measurable.measure(Constraints())
        val areaWidth = width.roundToPx()
        val half = cover.size / 2
        val gap = Odstepy.xs.roundToPx()
        val left = (cover.x * areaWidth - label.width / 2f).roundToInt()
        val x = left.coerceIn(minimumValue = 0, maximumValue = (areaWidth - label.width).coerceAtLeast(0))
        val above = ((cover.y - half) * height.toPx()).roundToInt() - gap - label.height
        val below = ((cover.y + half) * height.toPx()).roundToInt() + gap
        layout(width = label.width, height = label.height) {
            label.place(x = x, y = if (above >= 0) above else below)
        }
    }

@StringRes
private fun CoverPlace.nameRes(): Int =
    when (this) {
        CoverPlace.TOP_LEFT -> R.string.zaslona_u_gory_po_lewej
        CoverPlace.TOP -> R.string.zaslona_u_gory
        CoverPlace.TOP_RIGHT -> R.string.zaslona_u_gory_po_prawej
        CoverPlace.LEFT -> R.string.zaslona_po_lewej
        CoverPlace.CENTER -> R.string.zaslona_posrodku
        CoverPlace.RIGHT -> R.string.zaslona_po_prawej
        CoverPlace.BOTTOM_LEFT -> R.string.zaslona_na_dole_po_lewej
        CoverPlace.BOTTOM -> R.string.zaslona_na_dole
        CoverPlace.BOTTOM_RIGHT -> R.string.zaslona_na_dole_po_prawej
    }

private fun decodeContext(context: CoroutineContext = Dispatchers.Default): CoroutineContext = context

private fun decodedForScreen(jpeg: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
    var sample = 1
    val longer = if (bounds.outWidth > bounds.outHeight) bounds.outWidth else bounds.outHeight
    while (longer / (sample * 2) >= SHOWN_PIXELS) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)?.asImageBitmap()
}
