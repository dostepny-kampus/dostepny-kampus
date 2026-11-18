/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Ikony
import io.github.dostepnykampus.core.designsystem.LARGE_FONT_SCALE
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.feature.map.R

/**
 * Wyjścia z arkusza na mapie V1.
 *
 * Wymagania: US2.8, NFR-15, NFR-53
 *
 * @property onClose „Zamknij”
 * @property onBack systemowe „Wstecz”
 * @property onBuilding wiersz budynku o podanym symbolu
 */
@Immutable
internal data class SheetExits(
    val onClose: () -> Unit,
    val onBack: () -> Unit = onClose,
    val onBuilding: (String) -> Unit = {},
)

/**
 * Wiersz budynku [symbol] pod nagłówkiem arkusza, który przez [onClick] otwiera kartę tego budynku; cały wiersz to
 * jeden cel dotyku z nazwą i czynnością dla TalkBack. Nazwa [name] z danych stoi pod napisem i dochodzi do opisu
 * dla TalkBack; [modifier] to wnętrze celu dotyku, np. odstęp od ramki.
 *
 * Wymagania: US2.8, NFR-17, NFR-18, NFR-20
 */
@Composable
internal fun BuildingLink(symbol: String, modifier: Modifier = Modifier, name: String? = null, onClick: () -> Unit) {
    val text = stringResource(R.string.mapa_budynek, symbol)
    val action = stringResource(R.string.mapa_otwiera_opis_budynku)
    val linkSemantics =
        Modifier.clearAndSetSemantics {
            contentDescription = listOfNotNull(text, name).joinToString(separator = ", ")
            role = Role.Button
            onClick(label = action) {
                onClick()
                true
            }
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CeleDotyku.minimum)
                .then(linkSemantics)
                .clickable(onClick = onClick)
                .then(modifier),
        verticalArrangement = Arrangement.Center,
    ) {
        LinkLine(text)
        name?.let {
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(text = it, style = MaterialTheme.typography.bodyLarge, color = color)
        }
    }
}

@Composable
private fun LinkLine(text: String) {
    val isLarge = LocalDensity.current.fontScale >= LARGE_FONT_SCALE
    Row(horizontalArrangement = Arrangement.spacedBy(Odstepy.s), verticalAlignment = Alignment.CenterVertically) {
        if (!isLarge) LinkIcon(MapControlIcons.Building)
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        LinkIcon(MapControlIcons.Right)
    }
}

@Composable
private fun LinkIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(Ikony.rozmiar),
        tint = MaterialTheme.colorScheme.primary,
    )
}
