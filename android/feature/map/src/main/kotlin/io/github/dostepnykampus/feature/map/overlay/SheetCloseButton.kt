/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.feature.map.R

/**
 * Obrysowany „Zamknij” w nagłówku arkusza na mapie, z celem dotyku [CeleDotyku.minimum].
 *
 * Wymagania: NFR-17, NFR-53
 *
 * @param onClose zamyka arkusz
 * @param modifier modyfikator zewnętrzny
 */
@Composable
internal fun SheetCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick = onClose, modifier = modifier.heightIn(min = CeleDotyku.minimum)) {
        Text(stringResource(R.string.mapa_zamknij))
    }
}
