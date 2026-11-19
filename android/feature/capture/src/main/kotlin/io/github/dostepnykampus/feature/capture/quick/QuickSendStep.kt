/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.quick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.core.designsystem.Typografia
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.capture.R
import io.github.dostepnykampus.feature.capture.navigation.ReportPlace
import io.github.dostepnykampus.feature.capture.photo.PhotoPaths
import io.github.dostepnykampus.feature.capture.photo.PhotoPreviews

/**
 * Krok 3 V4 przed „Wyślij”: miniatura zdjęcia, wybrana grupa, piętro albo „Nie wiem”, jeśli było pytanie,
 * i zdanie o dołączeniu bieżącego miejsca albo, przy wybranym wejściu lub obiekcie planu, napis o nim. Gdy
 * pozycja jest jeszcze nieznana, zamiast zdania „Ustalamy, gdzie jesteś…”; „Wyślij” wtedy czeka. Przy „Brak
 * pozycji” i pozycji poza terenem PWr komunikat mówi, co zrobić; zdjęcie i grupa zostają.
 *
 * Wymagania: US4.1, US4.5, NFR-27, NFR-23, UC7, US4.9
 *
 * @param report zgłoszenie po kroku 2
 * @param isFloorAsked czy krok 2 pytał o piętro
 * @param lookup ustalanie bieżącej pozycji
 */
@Composable
internal fun QuickSendSummary(report: QuickReport, isFloorAsked: Boolean, lookup: QuickLookup) {
    Column(verticalArrangement = Arrangement.spacedBy(Odstepy.s)) {
        PhotoPreviews(PhotoPaths(listOf(report.photo.path)))
        Text(stringResource(report.group.quickLabel()))
        if (isFloorAsked) Text(report.floorLabel ?: floorLabel(report.floorNumber))
        when (lookup) {
            is QuickLookup.Found -> {
                val entrance = lookup.position.entrance
                val place = lookup.position.place
                val current = stringResource(R.string.szybkie_dolaczymy_miejsce)
                Text(
                    when {
                        entrance != null -> entranceLine(entrance)
                        place != null -> placeLine(place)
                        else -> current
                    },
                )
            }

            QuickLookup.Locating -> {
                Text(stringResource(R.string.szybkie_ustalamy_miejsce))
            }

            QuickLookup.Lost -> {
                LookupProblem(stringResource(R.string.szybkie_brak_pozycji))
            }

            QuickLookup.Outside -> {
                LookupProblem(stringResource(R.string.szybkie_poza_terenem))
            }
        }
    }
}

@Composable
private fun LookupProblem(text: String) {
    Text(
        text = text,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        color = MaterialTheme.colorScheme.error,
        fontWeight = Typografia.wyroznienieWaga,
    )
}

/**
 * Potwierdzenie po „Wyślij”: zgłoszenie trafi do weryfikacji, dalszego statusu Odwiedzający nie widzi.
 * Bez internetu: zdjęcie czeka w telefonie i wyśle się samo po powrocie sieci.
 *
 * Wymagania: US4.4, US4.2, NFR-5
 *
 * @param isSavedOffline zapis w kolejce bez internetu
 */
@Composable
internal fun QuickSentMessage(isSavedOffline: Boolean = false) {
    Text(stringResource(if (isSavedOffline) R.string.szybkie_zapisane_bez_sieci else R.string.szybkie_dziekujemy))
}

@Composable
@ReadOnlyComposable
private fun floorLabel(floor: Int?): String =
    when (floor) {
        null -> stringResource(R.string.szybkie_nie_wiem)
        0 -> stringResource(R.string.szybkie_parter)
        else -> stringResource(R.string.szybkie_pietro, floor)
    }

/**
 * Napis o wejściu [entrance] w krokach 1 i 3 V4: nazwa, „Wejście główne” albo wejście bez nazwy. Przy nazwie
 * nadanej przez import napis ma też opis położenia, bo kilka wejść może się nazywać „Wejście”.
 *
 * Wymagania: US4.6, BR-27
 */
@Composable
@ReadOnlyComposable
internal fun entranceLine(entrance: Entrance): String {
    val name = entrance.name
    val place = entrance.placeDescription.removeSuffix(".").replaceFirstChar(Char::lowercaseChar)
    return when {
        name != null && entrance.isNameGenerated && place.isNotEmpty() -> {
            stringResource(R.string.szybkie_wejscie_polozenie, name, place)
        }

        name != null -> {
            stringResource(R.string.szybkie_wejscie, name)
        }

        entrance.isMain -> {
            stringResource(R.string.szybkie_wejscie_glowne)
        }

        else -> {
            stringResource(R.string.szybkie_wejscie_bez_nazwy)
        }
    }
}

/**
 * Napis o obiekcie planu [place] w krokach 1 i 3 V4: nazwa obiektu, budynek i piętro planu, np. „Toaleta
 * dostosowana: Budynek A-1, parter”.
 *
 * Wymagania: US4.6, BR-8
 */
@Composable
@ReadOnlyComposable
internal fun placeLine(place: ReportPlace): String {
    val floor = place.floorLabel.replaceFirstChar(Char::lowercaseChar)
    val name =
        when (place.category) {
            RoomCategory.STAIRCASE -> stringResource(R.string.szybkie_obiekt_schody)

            RoomCategory.ELEVATOR -> stringResource(R.string.szybkie_obiekt_winda)

            RoomCategory.ACCESSIBLE_TOILET -> stringResource(R.string.szybkie_obiekt_toaleta)

            RoomCategory.CLASSROOM,
            RoomCategory.LECTURE_HALL,
            RoomCategory.CORRIDOR,
            RoomCategory.TOILET,
            RoomCategory.OFFICE,
            RoomCategory.OTHER,
            -> stringResource(R.string.szybkie_obiekt_inny)
        }
    return stringResource(R.string.szybkie_obiekt, name, place.buildingSymbol, floor)
}
