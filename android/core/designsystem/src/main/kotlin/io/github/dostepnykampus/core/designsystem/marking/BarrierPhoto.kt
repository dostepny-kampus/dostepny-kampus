/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.core.model.submission.BarrierType

private const val PHOTO_ASPECT_RATIO = 4f / 3f
private val PhotoMaxHeight = 240.dp

/**
 * Zdjęcie zgłoszenia bariery z adresu [url]. Bez adresu nie rysuje niczego. Rezerwuje stałą ramkę
 * o proporcjach 4:3 (do [PhotoMaxHeight]) na tle `surfaceContainer`, aby treść pod zdjęciem nie
 * przesuwała się podczas wczytywania; po błędzie pokazuje komunikat z ponowieniem.
 *
 * Wymagania: US1.5, US2.2, US8.4, BR-17, BR-19, NFR-12, NFR-18, NFR-20, NFR-50
 *
 * @param url adres zdjęcia albo `null`, gdy zgłoszenie nie ma zdjęcia
 * @param type typ bariery do opisu obrazu; `null` daje ogólny opis
 * @param modifier modyfikator obrazu
 */
@Composable
public fun BarrierPhoto(url: String?, type: BarrierType?, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) return
    val typeName = stringResource(type?.nameRes() ?: R.string.bariera_bez_typu)
    val description = stringResource(R.string.bariera_zdjecie, typeName)
    var attempt by rememberSaveable(url) { mutableIntStateOf(0) }
    val shape = RoundedCornerShape(Zaokraglenia.srednie)
    val background = MaterialTheme.colorScheme.surfaceContainer
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val frameHeight = (maxWidth / PHOTO_ASPECT_RATIO).coerceAtMost(PhotoMaxHeight)
        key(attempt) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = description,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
                loading = {
                    PhotoSlot(
                        modifier = Modifier.fillMaxWidth().height(frameHeight),
                        shape = shape,
                        background = background,
                    ) {
                        PhotoNote(stringResource(R.string.zdjecie_wczytujemy))
                    }
                },
                error = {
                    PhotoSlot(
                        modifier = Modifier.fillMaxWidth().heightIn(min = frameHeight),
                        shape = shape,
                        background = background,
                    ) {
                        PhotoError(onRetry = { attempt++ })
                    }
                },
                success = {
                    SubcomposeAsyncImageContent(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(frameHeight)
                                .clip(shape)
                                .background(background),
                    )
                },
            )
        }
    }
}

@Composable
private fun PhotoSlot(
    shape: RoundedCornerShape,
    background: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.clip(shape).background(background).padding(Odstepy.m),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun PhotoError(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        PhotoNote(stringResource(R.string.zdjecie_blad))
        OutlinedAction(onClick = onRetry) {
            Text(stringResource(R.string.sprobuj_ponownie))
        }
    }
}

@Composable
private fun PhotoNote(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}
