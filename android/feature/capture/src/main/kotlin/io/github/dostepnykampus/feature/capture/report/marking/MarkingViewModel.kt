/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.submission.sync.PendingPhotoDraft
import io.github.dostepnykampus.core.data.submission.sync.SaveRefusedException
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.capture.navigation.MarkingRoute
import io.github.dostepnykampus.feature.capture.report.ReportSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Model widoku C6 „Zmiana oznaczenia wejścia”. Zmiana wymaga wyboru „dostępne” albo „niedostępne” i zdjęcia
 * wejścia; bez nich nic się nie zapisuje. Zapis idzie tą samą ścieżką co formularz C2 i czeka w telefonie
 * na wysłanie.
 * TODO(#3676): ekran C6 i przejście z karty budynku.
 *
 * Wymagania: US17.2, BR-24, BR-7, NFR-7
 *
 * @param catalog katalog budynków z wejściem, jego obecnym oznaczeniem i nazwą budynku
 * @param saver zapis w kolejce telefonu
 */
@HiltViewModel
class MarkingViewModel
    @Inject
    constructor(
        private val catalog: BuildingCatalogRepository,
        private val saver: ReportSaver,
    ) : ViewModel() {
        private val state = MutableStateFlow(MarkingUiState())
        private var requested: MarkingRoute? = null

        /** Stan ekranu. */
        val uiState: StateFlow<MarkingUiState> = state.asStateFlow()

        /** Wczytuje wejście z [route]; kolejne wywołania nic nie zmieniają, więc obrót nie kasuje wyboru i zdjęcia. */
        fun open(route: MarkingRoute) {
            if (requested != null) return
            requested = route
            viewModelScope.launch {
                val found = catalogOrNull()
                val entrance = found?.run { entrances.firstOrNull { it.matches(route) } }
                val building = found?.run { buildings.firstOrNull { it.symbol == route.buildingSymbol } }
                val stage = if (entrance == null) MarkingStage.MISSING else MarkingStage.EDITING
                state.update { it.copy(entrance = entrance, buildingName = building?.name, stage = stage) }
            }
        }

        /** Wybór nowego oznaczenia; „brak danych” nadaje tylko import, więc nie zmienia wyboru. Wymagania: BR-24 */
        fun choose(marking: EntranceMarking) {
            if (marking == EntranceMarking.NO_DATA) return
            state.update { if (it.isEditable) it.copy(chosen = marking, isChoiceMissing = false) else it }
        }

        /** Zdjęcie wejścia; kolejne zastępuje poprzednie. Wymagania: US17.2, BR-24 */
        fun takePhoto(photo: PendingPhotoDraft) {
            state.update { if (it.isEditable) it.copy(photo = photo, isPhotoMissing = false) else it }
        }

        /** „Wyślij do sprawdzenia” i „Spróbuj ponownie”: bez wyboru albo zdjęcia błąd walidacji, inaczej zapis. */
        fun send() {
            val current = state.value
            val entrance = current.entrance
            if (!current.isEditable || entrance == null) return
            val chosen = current.chosen
            val photo = current.photo
            if (chosen == null || photo == null) {
                state.value = current.copy(isChoiceMissing = chosen == null, isPhotoMissing = photo == null)
                return
            }
            state.value = current.copy(stage = MarkingStage.SAVING)
            viewModelScope.launch {
                val stage =
                    try {
                        saver.save(entrance.markingContent(chosen, photo), listOf(photo), null)
                        MarkingStage.SAVED
                    } catch (error: IOException) {
                        Timber.w("Zapis zmiany oznaczenia wejścia nieudany: %s", error.javaClass.simpleName)
                        MarkingStage.SAVE_FAILED
                    } catch (error: SQLiteException) {
                        Timber.w("Zapis zmiany oznaczenia wejścia nieudany: %s", error.javaClass.simpleName)
                        MarkingStage.SAVE_FAILED
                    } catch (_: SaveRefusedException) {
                        MarkingStage.SAVE_REJECTED
                    }
                state.update { it.copy(stage = stage) }
            }
        }

        private suspend fun catalogOrNull(): BuildingCatalog? =
            try {
                catalog.catalog()
            } catch (error: IOException) {
                Timber.w("Katalog budynków dla zmiany oznaczenia wejścia niedostępny: %s", error.javaClass.simpleName)
                null
            } catch (error: SQLiteException) {
                Timber.w("Katalog budynków dla zmiany oznaczenia wejścia niedostępny: %s", error.javaClass.simpleName)
                null
            }
    }

/** Czy to wejście z [route]: ten sam identyfikator i budynek. Wymagania: US17.2 */
internal fun Entrance.matches(route: MarkingRoute): Boolean =
    id == route.entranceId && buildingSymbol == route.buildingSymbol
