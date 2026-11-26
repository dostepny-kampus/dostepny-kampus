/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.R
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.core.model.submission.BarrierType

private val PhotoMaxHeight = 240.dp

/**
 * Zdjęcie zgłoszenia bariery z adresu [url]. Bez adresu nie rysuje niczego. W miejscu zdjęcia stoi napis
 * o wczytywaniu, a po błędzie komunikat z ponowieniem.
 *
 * Wymagania: US1.5, US2.2, US8.4, BR-17, BR-19, NFR-12, NFR-18, NFR-50
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
    key(attempt) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = description,
            modifier =
                modifier
                    .fillMaxWidth()
                    .heightIn(max = PhotoMaxHeight)
                    .clip(RoundedCornerShape(Zaokraglenia.srednie)),
            contentScale = ContentScale.Fit,
            loading = { PhotoNote(stringResource(R.string.zdjecie_wczytujemy)) },
            error = { PhotoError(onRetry = { attempt++ }) },
        )
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
