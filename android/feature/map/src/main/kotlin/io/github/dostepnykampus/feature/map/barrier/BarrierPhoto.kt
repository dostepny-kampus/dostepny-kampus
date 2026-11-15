/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.designsystem.marking.nameRes
import io.github.dostepnykampus.feature.map.R

private val PhotoMaxHeight = 240.dp

/**
 * Zdjęcie zgłoszenia bariery z adresu [io.github.dostepnykampus.core.data.barrier.AcceptedBarrier.photoUrl].
 * Bez adresu nie rysuje niczego. Obraz ma opis z typem bariery; w miejscu zdjęcia stoi napis o wczytywaniu,
 * a po błędzie komunikat z przyciskiem ponowienia.
 *
 * Wymagania: US1.5, US2.2, BR-17, NFR-12, NFR-18, NFR-50
 *
 * @param chosen bariera, której zdjęcie się pokazuje
 */
@Composable
internal fun BarrierPhoto(chosen: ChosenBarrier) {
    val url = chosen.barrier.photoUrl ?: return
    val type = stringResource(chosen.barrier.type?.nameRes() ?: R.string.mapa_bariera_bez_typu)
    val description = stringResource(R.string.mapa_bariera_zdjecie, type)
    var attempt by rememberSaveable(url) { mutableIntStateOf(0) }
    key(attempt) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = description,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = PhotoMaxHeight)
                    .clip(RoundedCornerShape(Zaokraglenia.srednie)),
            contentScale = ContentScale.Fit,
            loading = { PhotoNote(stringResource(R.string.mapa_zdjecie_wczytujemy)) },
            error = { PhotoError(onRetry = { attempt++ }) },
        )
    }
}

@Composable
private fun PhotoError(onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        PhotoNote(stringResource(R.string.mapa_zdjecie_blad))
        OutlinedButton(onClick = onRetry, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.karta_sprobuj))
        }
    }
}

@Composable
private fun PhotoNote(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}
