/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.CeleDotyku
import io.github.dostepnykampus.core.designsystem.Obramowania
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.designsystem.Zaokraglenia
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.feature.map.R
import io.github.dostepnykampus.feature.map.overlay.BuildingLink
import io.github.dostepnykampus.feature.map.overlay.MapSheet

/**
 * Arkusz „Który budynek?” na dole mapy po dotknięciu konturu wspólnego albo dwóch konturów w zasięgu 24 dp.
 * Każdy budynek to wiersz budynku w ramce na pełną szerokość, jak w innych arkuszach mapy, z nazwą z danych pod
 * napisem. Przy dużej czcionce arkusz się przewija. Nagłówek „Który budynek?” ma obok obrysowany przycisk
 * „Zamknij”.
 *
 * Wymagania: US2, NFR-17, NFR-18, NFR-20, NFR-53
 *
 * @param onClose „Zamknij” i „Wstecz” zamykają arkusz
 * @param modifier położenie arkusza na mapie
 * @param content wiersze [ChoiceLink] budynków trafionych dotknięciem, w kolejności podpisu konturu
 */
@Composable
internal fun BuildingChoiceSheet(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    MapSheet(modifier) {
        SheetHeader(onClose)
        content()
    }
}

@Composable
private fun SheetHeader(onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.mapa_ktory_budynek),
            modifier = Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = CeleDotyku.minimum)) {
            Text(stringResource(R.string.mapa_zamknij))
        }
    }
}

/**
 * Karta budynku [building] pod polem symbolu i na liście budynków:
 * symbol na plakietce, nazwa z danych, a gdy nazwa jest, pod nią „Budynek C-3”; bez nazwy samo „Budynek C-3”.
 * Cała karta to jeden cel dotyku, a TalkBack czyta „C-3, nazwa” i czynność „otwiera opis budynku”. Wynik
 * wyszukiwania z innego terenu niż pokazany na mapie ma pod spodem dopisek „Wittigowo · inny teren”, a TalkBack
 * dodaje „teren Wittigowo, inny teren”.
 *
 * Wymagania: US2, US1.1, NFR-19, US1.8, NFR-18, NFR-21
 *
 * @param building budynek z katalogu: symbol i nazwa z danych, gdy jest
 * @param otherArea nazwa terenu budynku, gdy to inny teren niż pokazany na mapie
 * @param onClick otwiera V2 tego budynku
 */
@Composable
internal fun BuildingChoice(building: Building, otherArea: String? = null, onClick: () -> Unit) {
    val spokenArea = otherArea?.let { stringResource(R.string.mapa_teren_inny_opis, it) }
    ResultCard(
        symbol = building.symbol,
        description = listOfNotNull(building.symbol, building.name, spokenArea).joinToString(separator = ", "),
        action = stringResource(R.string.mapa_otwiera_opis_budynku),
        onClick = onClick,
    ) {
        val generic = stringResource(R.string.mapa_budynek, building.symbol)
        Text(
            text = building.name ?: generic,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = Typografia.wyroznienieWaga,
        )
        if (building.name != null) {
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(text = generic, style = MaterialTheme.typography.bodyLarge, color = color)
        }
        otherArea?.let { OtherAreaNote(it) }
    }
}

/**
 * Karta wyniku w ramce: znak budynku [symbol] i obok [content]. Cała karta to jeden cel dotyku [onClick],
 * a TalkBack czyta [description] i czynność [action], gdy jest.
 *
 * Wymagania: NFR-17, NFR-18
 */
@Composable
internal fun ResultCard(
    symbol: String,
    description: String,
    action: String?,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Zaokraglenia.duze)
    val choiceSemantics =
        Modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.Button
            onClick(label = action) {
                onClick()
                true
            }
        }
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(choiceSemantics)
                .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.heightIn(min = CeleDotyku.minimum).padding(Odstepy.m),
            horizontalArrangement = Arrangement.spacedBy(Odstepy.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolBadge(symbol)
            Column(content = content)
        }
    }
}

/**
 * Wiersz budynku [building] w ramce w arkuszu „Który budynek?”: znak budynku, „Budynek C-3”, strzałka i nazwa
 * z danych pod spodem; [onClick] wybiera ten budynek.
 *
 * Wymagania: US2, US2.8, NFR-17, NFR-18
 */
@Composable
internal fun ChoiceLink(building: Building, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Zaokraglenia.duze)
    Surface(
        modifier = Modifier.fillMaxWidth().clip(shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(Obramowania.ramka, MaterialTheme.colorScheme.outlineVariant),
    ) {
        BuildingLink(
            symbol = building.symbol,
            modifier = Modifier.padding(Odstepy.m),
            name = building.name,
            onClick = onClick,
        )
    }
}

@Composable
private fun SymbolBadge(symbol: String) {
    Surface(shape = RoundedCornerShape(Zaokraglenia.srednie), color = MaterialTheme.colorScheme.primaryContainer) {
        Box(
            modifier = Modifier.defaultMinSize(minWidth = CeleDotyku.przycisk, minHeight = CeleDotyku.minimum),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = symbol,
                modifier = Modifier.padding(horizontal = Odstepy.s),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = Typografia.wyroznienieWaga,
            )
        }
    }
}

@Composable
private fun OtherAreaNote(area: String, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.mapa_teren_inny, area),
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = Typografia.wyroznienieWaga,
    )
}
