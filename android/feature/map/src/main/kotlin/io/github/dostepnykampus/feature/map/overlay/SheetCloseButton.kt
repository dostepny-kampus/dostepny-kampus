/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.card.OutlinedAction
import io.github.dostepnykampus.feature.map.R

/**
 * Obrysowany „Zamknij” w nagłówku arkusza na mapie, z celem dotyku [CeleDotyku.minimum]; z [back] zamiast niego
 * powrót do miejsca, z którego arkusz otwarto.
 *
 * Wymagania: NFR-17, NFR-18, NFR-53
 *
 * @param onClose zamyka arkusz albo wraca do [back]
 * @param modifier modyfikator zewnętrzny
 * @param back miejsce powrotu; `null` daje „Zamknij”
 */
@Composable
internal fun SheetCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier, back: SheetBack? = null) {
    val spoken = back?.spoken
    val described = if (spoken == null) Modifier else Modifier.semantics { contentDescription = spoken }
    OutlinedAction(onClick = onClose, modifier = modifier.heightIn(min = CeleDotyku.minimum).then(described)) {
        if (back == null) {
            Text(stringResource(R.string.mapa_zamknij))
        } else {
            Icon(imageVector = MapControlIcons.Left, contentDescription = null)
            Text(text = back.label, modifier = Modifier.clearAndSetSemantics {})
        }
    }
}
